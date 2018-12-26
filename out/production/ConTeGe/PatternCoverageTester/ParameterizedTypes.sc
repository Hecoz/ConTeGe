class Phone{
	def name = "phone"

	override def toString = "phone"
}

class Apple extends Phone{
	override def name = "Apple"

	override def toString = "Apple"
}

class HuaWei extends Phone{
	override def name = "HuaWei"
	override def toString = "HuaWei"
}

class Iphone extends Apple{
	override def name = "Iphone"
	override def toString = "Iphone"
}
//协变
class Covariant[+T]

//String 是 AnyRef 的子类
val covariant: Covariant[AnyRef] = new Covariant[String]

//逆变
class Contravariant[-T]

val contravariant: Contravariant[String] = new Contravariant[AnyRef]

//下界
class Consumer[+T](t: T){
	//异常，因为如果
	//def name(t: T){}
	def name[U >: T](u: U){}
}

class MyConsumer[T](t: T){
	def name: Unit ={
		println(t)
	}
}

var c1 = new MyConsumer[Phone](new Apple)
println(c1.name)
var c2 = new MyConsumer[Apple](new Iphone)
println(c2.name)
//c1 = c2

var c3 = new Consumer[Phone](new Apple)
var c4 = new Consumer[Apple](new Iphone)
c3 = c4

var c5 = new Consumer[Phone](new Phone)
var c6 = new Consumer[Apple](new Apple)
c5 = c6


class DownPhoneShop[T >: Apple](t: T){

}

var p = new DownPhoneShop[AnyRef](new AnyRef)

var p1 = new DownPhoneShop[Phone](new Phone)
//var p2 = new DownPhoneShop[Apple](new Iphone)
//var p3 = new DownPhoneShop[Iphone](new Iphone)
//
////p1 = p2
//class CovariantPhoneShop[+T >: Phone]
//
//var p4 = new CovariantPhoneShop[Phone]
//var p5 = new CovariantPhoneShop[Apple]
//p4 = p5
//
////上界s
//class UpperPhoneShop[T <: Phone]
//var u1 = new UpperPhoneShop[Phone]
//var u2 = new UpperPhoneShop[Apple]