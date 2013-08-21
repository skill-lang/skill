trait Date {
//////// FIELD INTERACTION ////////
def date(): Long
def setDate(date: Long):Unit
//////// OBJECT CREATION AND DESTRUCTION ////////
//////// UTILS ////////

override def toString(): String = "Date(+date+", "+)
}