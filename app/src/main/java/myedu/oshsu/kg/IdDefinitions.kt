package myedu.oshsu.kg

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object IdDefinitions {

    var countries: Map<Int, DictionaryItem> = emptyMap()
    var oblasts: Map<Int, DictionaryItem> = emptyMap()
    var regions: Map<Int, DictionaryItem> = emptyMap()
    var nationalities: Map<Int, DictionaryItem> = emptyMap()
    var schools: Map<Int, DictionaryItem> = emptyMap()
    var genders: Map<Int, DictionaryItem> = emptyMap()
    var periods: Map<Int, PeriodItem> = emptyMap()
    
    var isLoaded = false

    suspend fun loadAll() = withContext(Dispatchers.IO) {
        if (isLoaded) return@withContext
        try {
            countries = safeCall { NetworkClient.api.getCountries() }.associateBy { it.id }
            oblasts = safeCall { NetworkClient.api.getOblasts() }.associateBy { it.id }
            regions = safeCall { NetworkClient.api.getRegions() }.associateBy { it.id }
            nationalities = safeCall { NetworkClient.api.getNationalities() }.associateBy { it.id }
            schools = safeCall { NetworkClient.api.getSchools() }.associateBy { it.id }
            genders = safeCall { NetworkClient.api.getGenders() }.associateBy { it.id }
            periods = safeCall { NetworkClient.api.getPeriods() }.associateBy { it.id }
            isLoaded = true
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun <T> safeCall(call: suspend () -> List<T>): List<T> {
        return try { call() } catch (e: Exception) { emptyList() }
    }

    fun getCountryName(id: Int?, lang: String): String = countries[id]?.getName(lang) ?: "-"
    fun getOblastName(id: Int?, lang: String): String = oblasts[id]?.getName(lang) ?: "-"
    fun getRegionName(id: Int?, lang: String): String = regions[id]?.getName(lang) ?: "-"
    
    fun getPeriodName(id: Int?, lang: String): String {
        return periods[id]?.getName(lang) ?: "-"
    }
}
