package contege

import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.FileReader
import java.io.PrintStream
import java.lang.reflect.InvocationTargetException
import java.util.ArrayList
import java.util.Date
import scala.collection.JavaConversions._
import scala.collection.mutable.Map
import scala.collection.mutable.Set
import contege.seqexec.DeadlockMonitor
import contege.seqgen.InstantiateCutTask
import contege.seqgen.Prefix
import contege.seqgen.StateChangerTask
import contege.seqgen.Suffix
import contege.seqgen.SuffixGen
import contege.seqgen.TypeManager
import javamodel.util.TypeResolver
import contege.seqexec.TestPrettyPrinter
import contege.seqexec.jpf.JPFFirstSequenceExecutor
import contege.seqexec.jpf.TSOracleJPFFirst
import java.io.File
import contege.seqexec.reflective.TSOracleNormalExec
import contege.seqexec.reflective.SequenceManager
import contege.seqexec.reflective.SequenceExecutor
import scala.util.control.Exception.allCatch

class ClassTester(config: Config,
                  stats: Stats,
                  putClassLoader: ClassLoader,
                  putJarPath: String,
                  envTypes: ArrayList[String],
                  random: Random,
                  finalizer: Finalizer) {

    // PLDI 2012
    private val cutCallsPerSeq = 2
    private val maxSuffixLength = 10
    private val concRunRepetitions = 100
    private val maxStateChangersInPrefix = 5

    private val prefixes = new ArrayList[Prefix] // kept separately to ensure deterministic random selection of one
    private val prefix2SuffixGen = Map[Prefix, SuffixGen]()
    private val prefix2Suffixes = Map[Prefix, ArrayList[Suffix]]()

    private val typeProvider = new TypeManager(config.cut, envTypes, putClassLoader, random)

    private val seqMgr = new SequenceManager(new SequenceExecutor(stats, config), config, finalizer)

    private val global = new GlobalState(config, typeProvider, seqMgr, stats, random, finalizer)

    private val tsOracle = if (config.useJPFFirst) {
        val jpfFirstExecutor = new JPFFirstSequenceExecutor(global, putJarPath)
        new TSOracleJPFFirst(finalizer, stats, config, seqMgr.seqExecutor, jpfFirstExecutor)
    } else {
        new TSOracleNormalExec(finalizer, concRunRepetitions, stats, seqMgr.seqExecutor, config)
    }

    // hack to always go through the PUT class loader -- ideally TypeResolver would be a class instead of a singleton 
    TypeResolver.bcReader.classLoader = putClassLoader

    def run: Unit = {

        //check deadlock
        val dlMonitor = new DeadlockMonitor(config, global)
        dlMonitor.setDaemon(true)
        dlMonitor.start

        var nbGeneratedTests = 0L
        config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))

        for (suffixGenTries <- 1 to config.maxSuffixGenTries) { // generate call sequences
            stats.timer.start("gen")
            val prefix = getPrefix
            stats.timer.stop("gen")

            val suffixGen = prefix2SuffixGen(prefix)
            val suffixes = prefix2Suffixes(prefix)

            stats.timer.start("gen")
            val nextSuffixOpt = suffixGen.nextSuffix(cutCallsPerSeq)
            stats.timer.stop("gen")
            nextSuffixOpt match {
                case Some(suffix) => {
                    assert(suffix.length > 0)
                    if (!suffixes.exists(oldSuffix => suffix.equivalentTo(oldSuffix))) {
                        suffixes += suffix
                        println("New suffix \n" + suffix)
                        // run the new sequences against all existing sequences and again itself
                        suffixes.foreach(otherSuffix => {
                            nbGeneratedTests += 1
                            config.checkerListeners.foreach(l => l.updateNbGeneratedTests(nbGeneratedTests))
                            println("Nb generated tests: " + nbGeneratedTests)

                            finalizer.currentTest = Some(TestPrettyPrinter.javaCodeFor(prefix, suffix, otherSuffix, "GeneratedTest", TestPrettyPrinter.NoOutputVectors))

                            tsOracle.analyzeTest(prefix, suffix, otherSuffix)
                        })
                    }
                    if (suffixes.size % 5 == 0) {
                        stats.print
                        stats.timer.print
                    }
                }
                case None => // ignore
            }
        }

        println("ClassTester: Could not find bug.")	
    }

    private def getPrefix: Prefix = {

        if (prefixes.size < config.maxPrefixes) { // try to create a new prefix

            new InstantiateCutTask(global).run match {

                case Some(prefix) => {
                    prefix.fixCutVariable
                    assert(prefix.types.contains(config.cut), prefix.types)
                    if (prefixes.exists(oldPrefix => prefix.equivalentTo(oldPrefix))) { // we re-created an old prefix, return a random existing prefix 
                        return prefixes(random.nextInt(prefixes.size))
                    } else {
                        val extendedPrefix = appendStateChangers(prefix)
                        prefixes.add(extendedPrefix)
                        prefix2SuffixGen.put(extendedPrefix, new SuffixGen(extendedPrefix, maxSuffixLength, global))
                        prefix2Suffixes.put(extendedPrefix, new ArrayList[Suffix]())
                        println("New prefix:\n" + extendedPrefix)
                        return extendedPrefix
                    }
                }
                case None => {
                    if (prefixes.isEmpty) {
                        config.checkerListeners.foreach(_.appendResultMsg("Cannot instantiate " + config.cut + ". Is there a public constructor or method to create it?"))
                        finalizer.finalizeAndExit(false)
                        return null
                    } else prefixes(random.nextInt(prefixes.size))
                }
            }
        } else {
            return prefixes(random.nextInt(prefixes.size))
        }
    }

    private def appendStateChangers(prefix: Prefix) = {
        var currentSequence = prefix
        for (_ <- 1 to maxStateChangersInPrefix) {
            val stateChangerTask = new StateChangerTask(currentSequence, global)
            stateChangerTask.run match {
                case Some(extendedSequence) => {
                    currentSequence = extendedSequence
                }
                case None => // ignore
            }
        }
        currentSequence
    }
}

object ClassTester extends Finalizer {

    val startTime = System.currentTimeMillis
    var stats: Stats = _
    var config: Config = _

//    def main(args: Array[String]): Unit = {
//
//        println("Starting ClassTester at " + new Date())
//
//        //org.jfree.data.XYSeries ${testedJarEnvTypes} ${seed} ${maxSuffixGenTries} result.out false
//        /**
//          * 01 check arguements
//          */
//        assert(args.size == 6 || args.size == 7 || args.size ==8)
//
//        /**
//          * 02 args(0) set class Under Test
//          */
//        //val cut = args(0)
//        val cut  = "org.jfree.data.XYSeries"
//
//        if(args.size==8){
//
//            args(7) match {
//
//                case time if(isLong(time)) => scheduleTimer(time.toLong)
//                case _ => //do nothing as supplied string is not parseable to Integer
//            }
//        }
//        /**
//          * 03 args(2) set program seed
//          */
//        val seed = args(2).toInt
//        /**
//          * 04 args(3) set maxSuffixGenTries
//          */
//        val maxSuffixGenTries = args(3).toInt
//        assert(maxSuffixGenTries >= 2)
//        /**
//          * 05 args(5) set callClinit ???
//          */
//        val callClinit = args(5).toBoolean
//        /**
//          * 06 args(6) check whether cut methods have been selected
//          */
//        val selectedCUTMethods: Option[ArrayList[String]] = {
//
//            if (args.size == 7 && args(6)!="-1")
//                Some(readMethods(args(6)))
//            else
//                None
//        }
//        if (selectedCUTMethods.isDefined)
//            println("Focusing on " + selectedCUTMethods.get.size + " CUT methods")
//        else
//            println("No specific CUT methods selected")
//
//        /**
//          * 07 create config and use args(4) set result output dir
//          */
//        config = new Config(cut, seed, maxSuffixGenTries, selectedCUTMethods, new File("/tmp/"), callClinit)
//        val random = new Random(seed)
//        //args(4) result output
//        val resultFileName = args(4)
//        config.addCheckerListener(new ResultFileCheckerListener(resultFileName))
//        /**
//          * 08 ????
//          */
//        val envTypes = new ArrayList[String]
//        envTypes.add("java.lang.Object")
//        Util.addEnvTypes(args(1), envTypes, this.getClass.getClassLoader)
//
//        stats = new Stats
//        val tester = new ClassTester(config, stats, getClass.getClassLoader, ".", envTypes, random, this)
//        println("Testing " + cut + " with seed " + seed)
//
//        stats.timer.start("all")
//
//        tester.run
//
//        finalizeAndExit(false)
//    }

    def main(args: Array[String]): Unit = {

        println(System.getProperty("user.dir"))

        var cut  = "org.jfree.data.XYSeries"
        val seed = 3
        val maxSuffixGenTries = 100
        val callClinit = false
        val selectedCUTMethods: Option[ArrayList[String]] = None
        config = new Config(cut, seed, maxSuffixGenTries, selectedCUTMethods, new File("/tmp/"), callClinit)
        val random = new Random(seed)
        val resultFileName = "result.out"
        config.addCheckerListener(new ResultFileCheckerListener(resultFileName))

        val envTypes = new ArrayList[String]
        envTypes.add("java.lang.Object")

        /**
          * QUESTION1 : add environment dependency??? yes!!!
          */
        val envTypesPath: String = "./benchmarks/pldi2012/XYSeries/env_types.txt"
        Util.addEnvTypes(envTypesPath, envTypes, this.getClass.getClassLoader)

        stats = new Stats
        val tester = new ClassTester(config, stats, getClass.getClassLoader, ".", envTypes, random, this)

        println("Testing " + cut + " with seed " + seed)

        stats.timer.start("all")

        tester.run

        finalizeAndExit(false)
    }

    def isLong(argsStr: String): Boolean = (allCatch opt argsStr.toLong).isDefined

    //function to schedule timer to exit the program after given time in milliseconds.
    def scheduleTimer(time:Long){
      println("Timer scheduled for "+time+" milliseconds")
      val timer = new java.util.Timer()
        timer.schedule(new java.util.TimerTask(){
          def run() {
           println("Time exhausted, terminating the program.")
           finalizeAndExit(false)
           }    
        }, time)//1hr = 3600000
    }
    
    def finalizeAndExit(bugFound: Boolean) = {
        stats.timer.stop("all")
        stats.print
        stats.timer.print
        println("Done with ClassTester at " + new Date)
        val secondsTaken = (System.currentTimeMillis - startTime) / 1000
        config.checkerListeners.foreach(_.appendResultMsg("Time (seconds): " + secondsTaken))

        if (bugFound) {
            val testCode = currentTest.get
            config.checkerListeners.foreach(_.notifyDoneAndBugFound(testCode))
        } else config.checkerListeners.foreach(_.notifyDoneNoBug)

        System.exit(0)
    }

    private def readMethods(fileName: String) = {
        val result = new ArrayList[String]()
        val r = new BufferedReader(new FileReader(fileName))
        var line = r.readLine
        while (line != null) {
            result.add(line)
            line = r.readLine
        }
        r.close
        result
    }

}
