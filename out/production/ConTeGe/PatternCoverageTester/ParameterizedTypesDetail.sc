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

//下界 没有协变，
class DownPhoneShopNoCovariant[T >: Apple](t: T){

}
//下界，T必须是Apple的父类或自己，但是d1,d2之间没有继承关系,要加上协变才能赋值
var d1 = new DownPhoneShopNoCovariant[Phone](new Phone)
var d2 = new DownPhoneShopNoCovariant[Apple](new Apple)
//d1 = d2
//注意：！！！
//worksheet 在有错误时，如果后面有错误他会标到前面，可以尝试将下面一句的注释去掉
//var d3 = new DownPhoneShop[Iphone](new Iphone)

//下界 有协变
class DownPhoneShopCvariant[+T >: Apple](t: T){

}
var d4 = new DownPhoneShopCvariant[Phone](new Phone)
var d5 = new DownPhoneShopCvariant[Apple](new Apple)
d4 = d5

//上界 没有协变
class UpperPhoneShopNoCovariant[T <: Apple](t: T){

}
//上界，T必须是Apple的子类或者自己，且U2,U3之间无法赋值
//var u1 = new UpperPhoneShopNoCovariant[Phone](new Phone)
var u2 = new UpperPhoneShopNoCovariant[Apple](new Apple)
var u3 = new UpperPhoneShopNoCovariant[Iphone](new Iphone)
//u2 = u3

//上界 有协变
class UpperPhoneShopCovariant[+MyType <: Apple](myType: MyType){

}

var u4 = new UpperPhoneShopCovariant[Apple](new Apple)
var u5 = new UpperPhoneShopCovariant[Iphone](new Iphone)
u4 = u5

//逆变
//上界 有逆变
class UpperPhoneShopContravariant[-MyType <: Apple](myType: MyType){

}

var u6 = new UpperPhoneShopContravariant[Apple](new Apple)
var u7 = new UpperPhoneShopContravariant[Iphone](new Iphone)
//u6 = u7
u7 = u6


// - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -
class CPU[CpuType]

class GaoTong extends CPU[GaoTong]

class GaoTongNo1 extends GaoTong

class MobilePhone[CpuType <: CPU[CpuType]](cpuType: CpuType){

}

val apple = new MobilePhone[GaoTong](new GaoTong)

//val mi = new MobilePhone[GaoTongNo1](new GaoTongNo1)

//class CPU[CpuType]
//
//class GaoTong extends CPU[GaoTong]
//
//class GaoTongNo1 extends GaoTong
//
//class MobilePhone[CpuType <: CPU[CpuType]](cpuType: CpuType){
//
//}
//
//val apple = new MobilePhone[GaoTong](new GaoTong)
//val mi = new MobilePhone[GaoTongNo1](new GaoTongNo1)