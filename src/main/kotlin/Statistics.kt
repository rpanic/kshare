import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import java.io.File

class Statistics(var f: File){

    lateinit var statistic: Statistic

    lateinit var adapter: JsonAdapter<Statistic>

    fun init(){

        adapter = Moshi.Builder().build().adapter(Statistic::class.java)
        if(f.exists()) {
            var read = f.readLines().joinToString()
            statistic = adapter.fromJson(read) ?: Statistic(0, HashMap())
        }else{
            statistic = Statistic(0, HashMap())
        }

    }

    fun addVisit(url: String){

        statistic.visits++
        if(statistic.visitsPerSite[url] != null){
            statistic.visitsPerSite[url] = statistic.visitsPerSite[url]!! + 1;
        }else{
            statistic.visitsPerSite[url] = 1
        }

        save()

    }

    private fun save(){
        f.writeText(adapter.toJson(statistic))
    }

    data class Statistic(var visits: Int,
                         var visitsPerSite: MutableMap<String, Int>){
    }

}