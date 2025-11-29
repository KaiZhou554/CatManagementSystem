package com.kaizhou492.catmanagementsystem.ui

interface Strings {
    val appTitle: String
    val cattery: String
    val office: String
    val settings: String
    val autoFeeder: String
    val about: String
    val fillFood: String
    val fillWater: String
    val adoptCat: String
    val adoptionsLeft: String
    val dangerZone: String
    val giftCat: String
    val transferCattery: String
    val confirmAdopt: String
    val confirm: String
    val cancel: String
    val adoptSuccess: String
    val adoptLimitReached: String
    val giftSuccess: String
    val selectCatsToGift: String
    val confirmTransfer: String
    val transferSuccess: String
    val nameExists: String
    val invalidName: String
    val emptyName: String
    val editName: String
    val save: String
    val version: String
    val developer: String
    val noCats: String
}

object StringsZh : Strings {
    override val appTitle = "猫咪饲养管理"
    override val cattery = "猫舍"
    override val office = "办公室"
    override val settings = "设置"
    override val autoFeeder = "自动喂养器"
    override val about = "关于"
    override val fillFood = "填满猫粮碗"
    override val fillWater = "填满水盆"
    override val adoptCat = "收养猫咪"
    override val adoptionsLeft = "本周剩余收养次数"
    override val dangerZone = "危险操作区"
    override val giftCat = "赠送猫"
    override val transferCattery = "转让猫舍"
    override val confirmAdopt = "确认收养这只猫咪吗?"
    override val confirm = "确认"
    override val cancel = "取消"
    override val adoptSuccess = "收养成功!"
    override val adoptLimitReached = "本周收养次数已达上限(3只),下周可继续收养"
    override val giftSuccess = "赠送成功!"
    override val selectCatsToGift = "选择要赠送的猫咪"
    override val confirmTransfer = "确认转让猫舍?所有猫咪和本周收养记录将被清空,不可恢复"
    override val transferSuccess = "转让成功"
    override val nameExists = "名字已存在,请更换"
    override val invalidName = "名字不可包含特殊字符,请重新输入"
    override val emptyName = "名字不可为空"
    override val editName = "编辑名字"
    override val save = "保存"
    override val version = "版本 1.0.0"
    override val developer = "开发者: Claude"
    override val noCats = "还没有猫咪,快去收养吧!"
}

object StringsEn : Strings {
    override val appTitle = "Cat Management"
    override val cattery = "Cattery"
    override val office = "Office"
    override val settings = "Settings"
    override val autoFeeder = "Auto Feeder"
    override val about = "About"
    override val fillFood = "Fill Food Bowl"
    override val fillWater = "Fill Water Bowl"
    override val adoptCat = "Adopt Cat"
    override val adoptionsLeft = "Adoptions Left This Week"
    override val dangerZone = "Danger Zone"
    override val giftCat = "Gift Cat"
    override val transferCattery = "Transfer Cattery"
    override val confirmAdopt = "Confirm adoption?"
    override val confirm = "Confirm"
    override val cancel = "Cancel"
    override val adoptSuccess = "Adoption successful!"
    override val adoptLimitReached = "Weekly adoption limit reached (3 cats), try again next week"
    override val giftSuccess = "Gift successful!"
    override val selectCatsToGift = "Select cats to gift"
    override val confirmTransfer = "Confirm transfer? All cats and adoption records will be cleared permanently"
    override val transferSuccess = "Transfer successful"
    override val nameExists = "Name already exists, please choose another"
    override val invalidName = "Name cannot contain special characters"
    override val emptyName = "Name cannot be empty"
    override val editName = "Edit Name"
    override val save = "Save"
    override val version = "Version 1.0.0"
    override val developer = "Developer: Claude"
    override val noCats = "No cats yet, adopt one now!"
}