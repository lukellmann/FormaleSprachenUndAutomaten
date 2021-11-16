package lib

import lib.Action.*


sealed interface Action {

    data class Shift(val canonicalCollectionElement: CanonicalCollectionElement) : Action {
        override fun toString() = "shift ${canonicalCollectionElement.setToString()}"
    }

    data class Reduce(val production: ContextFreeProduction) : Action {
        override fun toString() = "reduce $production"
    }

    object Accept : Action {
        override fun toString() = "accept"
    }
}


private fun ContextFreeGrammar.action(
    canonicalCollectionElement: CanonicalCollectionElement,
    terminal: Terminal,
): Action? {
    require(canonicalCollectionElement in canonicalCollection) {
        "${canonicalCollectionElement.setToString()} is no element of canonical collection"
    }
    require(terminal == EOF || terminal in terminalAlphabet) {
        "Terminal $terminal is no element of terminal alphabet ${terminalAlphabet.setToString()}"
    }

    val dotAndTerminal = Dot + terminal

    if (canonicalCollectionElement.any { it.right containsSubWord dotAndTerminal }) {
        goToTable[canonicalCollectionElement, terminal]?.let { return Shift(it) }
    }

    canonicalCollectionElement
        .firstOrNull { lr0Item ->
            lr0Item.nonterminalLeft != extendedStartSymbol
                    && lr0Item.right endsWith Dot
                    && (terminal == EOF || followTable[lr0Item.nonterminalLeft]?.contains(terminal) == true)
        }
        ?.let { lr0Item ->
            return Reduce(
                production = productions.first { production ->
                    production.nonterminalLeft == lr0Item.nonterminalLeft
                            && production.right == lr0Item.right[0 until lr0Item.right.lastIndex]
                }
            )
        }

    val sAndDot = startSymbol + Dot

    return if (
        terminal == EOF
        && canonicalCollectionElement.any { it.nonterminalLeft == extendedStartSymbol && it.right == sAndDot }
    ) Accept else null
}


val ContextFreeGrammar.actionTable by LazyExtensionProperty {
    Function2Table(
        domainX = canonicalCollection,
        domainY = terminalAlphabet + EOF,
        function = ::action,
        name = "Action",
        xToString = Set<*>::setToString,
        zToString = { it?.toString() ?: "⊥" },
    )
}
