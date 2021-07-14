package models

class Struct (
    val transportName: String,
    val fields: List<Field>,
    val description: String?,

)
{

    fun foo() {
        var test = "abc"
        var tt = test.let{

        }
    }
}