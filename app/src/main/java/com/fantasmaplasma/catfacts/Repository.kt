package com.fantasmaplasma.catfacts

import android.content.res.Resources
import java.util.*

class Repository {
    private val facts = Array(207) {""}
    var nextFactIdx = 0
        private set

    fun getNextFact(): String {
        if(nextFactIdx > facts.size || facts[nextFactIdx].isEmpty())
            nextFactIdx = 0
        val fact = facts[nextFactIdx]
        nextFactIdx++
        return fact
    }

    fun initFacts(resources: Resources) {
        if(facts[0].isNotEmpty()) return
        val factsTxt = Scanner(resources.openRawResource(R.raw.cat_facts))
        var idx = 0
        while(factsTxt.hasNext()) {
            facts[idx] = factsTxt.nextLine()
            idx++
        }
    }

    fun setFact(currentFactIdx: Int): String {
        this.nextFactIdx = currentFactIdx
        return getNextFact()
    }
}
