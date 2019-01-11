import io.kweb.Kweb
import io.kweb.dom.element.creation.tags.a
import io.kweb.dom.element.creation.tags.button
import io.kweb.dom.element.creation.tags.textArea
import io.kweb.dom.element.events.on
import io.kweb.dom.element.new
import io.kweb.state.KVar

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