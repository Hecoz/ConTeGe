import scala.util.control.Exception.allCatch

def isLong(argsStr: String): Boolean = (allCatch opt argsStr.toLong).isDefined
//def isLong(argsStr: String): Boolean = (allCatch.opt(argsStr.toLong)).isDefined

isLong("data 1998")

val t1 = "2018"

allCatch opt t1.toLong

allCatch.opt(t1.toLong)

val t2: Long = t1.toLong

println(s"$t2")

Predef.println(t2)

def myprint(mystring: String) = (Predef println mystring)

myprint("you dian yisi")

def testReturn(a: Int,b: Int): Int = {

  var c = a + b
  if(c > 5)
    c = 10
  c
}

testReturn(4,5)