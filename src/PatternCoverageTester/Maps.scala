package PatternCoverageTester

import java.util

import scala.collection.JavaConversions._
import scala.collection.mutable.Set
import scala.collection.mutable.Map
import java.util.ArrayList

import contege.ClassReader
import contege.Random
import contege.Atom
import contege.ConstructorAtom
import contege.MethodAtom
import contege.Util
import contege.Config
import javamodel.util.TypeResolver
import contege.FieldGetterAtom
import javamodel.staticc.UnknownType

import scala.collection.mutable.Set

object Maps {

	def main(args: Array[String]): Unit = {

		val map = Map("key1"->1,"key2"->2)
		println(map("key1"))
		map.foreach(println)
		map.getOrElse("key3",2)

		println("- - - - - - - - - - - - - - - - - - - - - - ")
		val map1 = Map[String,ArrayList[Int]]()
		map1.getOrElseUpdate("key1",new ArrayList[Int]).add(1)
		var tmp = map1.getOrElse("key2",new ArrayList[Int]).add(2)
		map1.foreach(println)
		println(tmp)
	}

}
