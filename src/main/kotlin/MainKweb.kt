import io.kweb.*;
import io.kweb.dom.element.creation.tags.*
import io.kweb.dom.element.events.on
import io.kweb.dom.element.new
import io.kweb.state.KVar
import io.kweb.state.persistent.logger

fun main(args: Array<String>) {

    var c = KVar("")
    var x = KVar(0)

    Kweb(port = 8091){
        doc.body.new {
            button().text(x.map { "$it" }).on.click { x.value++ }

            textArea().on.input {
                    //if(c.value != it)
                        c.value = it
                }
            a().text(c)
        }
    }

}