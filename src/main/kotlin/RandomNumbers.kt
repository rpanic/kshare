import io.kweb.shoebox.toArrayList
import java.io.File
import kotlin.random.Random

class RandomNumbers{

    private var file = File(System.getProperty("user.dir") + "/usednumbers.csv")
    private var usedNumbers = ArrayList<String>()

    fun init(){

        if(!usedNumbers.isEmpty())
            return

        if(file.exists())
            usedNumbers = file.readLines().joinToString(";").split(";").toArrayList()
    }

    fun getNewNumber() : String{

        val length = 6

        var s = ""

        while(s.length < length)
            s += ('a'..('a' + 26)).random()

        usedNumbers.add(s)
        save()
        return s

    }

    private fun save() {

        if(!file.exists()) file.createNewFile()
        file.writeText(usedNumbers.joinToString(";"))

    }

}