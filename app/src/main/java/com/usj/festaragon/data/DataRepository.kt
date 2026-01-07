package com.usj.festaragon.data

import android.content.Context
import com.usj.festaragon.model.Event
import com.usj.festaragon.model.Multimedia
import org.json.JSONArray
import org.json.JSONObject

object DataRepository {
    
    private var jsonData: JSONObject? = null
    private var eventsCache: List<Event>? = null
    private var categoriesCache: List<Category>? = null
    private var categoriesMap: Map<String, String>? = null
    private var organizersMap: Map<String, Organizer>? = null
    
    data class Category(
        val id: String,
        val name: String,
        val icon: String
    )
    
    data class Organizer(
        val id: String,
        val name: String,
        val contact: String
    )
    
    /**
     * Initialize the repository by loading JSON data from assets
     * Should be called once in SplashActivity
     */
    fun initialize(context: Context) {
        try {
            val jsonString = context.assets
                .open("data-pueblo.json")
                .bufferedReader()
                .use { it.readText() }
            
            jsonData = JSONObject(jsonString)
            
            // Pre-parse and cache categories, organizers, and events
            parseCategories()
            parseOrganizers()
            parseEvents()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * Check if repository is initialized
     */
    fun isInitialized(): Boolean = jsonData != null
    
    /**
     * Get raw JSON object
     */
    fun getJsonObject(): JSONObject? = jsonData
    
    /**
     * Get eventos JSONArray
     */
    fun getEventosArray(): JSONArray? {
        return jsonData?.optJSONArray("eventos")
    }
    
    /**
     * Get categorias JSONArray
     */
    fun getCategoriasArray(): JSONArray? {
        return jsonData?.optJSONArray("categorias")
    }
    
    /**
     * Get organizadores JSONArray
     */
    fun getOrganizadoresArray(): JSONArray? {
        return jsonData?.optJSONArray("organizadores")
    }
    
    /**
     * Get parsed categories
     */
    fun getCategories(): List<Category> {
        if (categoriesCache == null) {
            parseCategories()
        }
        return categoriesCache ?: emptyList()
    }
    
    /**
     * Get all parsed events
     */
    fun getEvents(): List<Event> {
        if (eventsCache == null) {
            parseEvents()
        }
        return eventsCache ?: emptyList()
    }
    
    /**
     * Parse categories from JSON
     */
    private fun parseCategories() {
        val categoriasArray = getCategoriasArray() ?: return
        val categoryList = mutableListOf<Category>()
        val categoryMap = mutableMapOf<String, String>()
        
        for (i in 0 until categoriasArray.length()) {
            val cat = categoriasArray.getJSONObject(i)
            val id = cat.getString("id")
            val name = cat.getString("nombre")
            categoryList.add(
                Category(
                    id = id,
                    name = name,
                    icon = cat.getString("icono")
                )
            )
            categoryMap[id] = name
        }
        
        categoriesCache = categoryList
        categoriesMap = categoryMap
    }
    
    /**
     * Parse organizers from JSON
     */
    private fun parseOrganizers() {
        val organizadoresArray = getOrganizadoresArray() ?: return
        val organizerMap = mutableMapOf<String, Organizer>()
        
        for (i in 0 until organizadoresArray.length()) {
            val org = organizadoresArray.getJSONObject(i)
            val id = org.getString("id")
            organizerMap[id] = Organizer(
                id = id,
                name = org.getString("nombre"),
                contact = org.getString("contacto")
            )
        }
        
        organizersMap = organizerMap
    }
    
    /**
     * Parse events from JSON
     */
    private fun parseEvents() {
        val eventosArray = getEventosArray() ?: return
        val eventList = mutableListOf<Event>()
        
        for (i in 0 until eventosArray.length()) {
            val evento = eventosArray.getJSONObject(i)
            eventList.add(createEventFromJsonObject(evento))
        }
        
        eventsCache = eventList
    }
    
    /**
     * Create Event object from JSONObject
     */
    private fun createEventFromJsonObject(jsonObject: JSONObject): Event {
        // Parse multimedia array
        val multimediaArray = jsonObject.optJSONArray("multimedia") ?: JSONArray()
        val multimediaList = mutableListOf<Multimedia>()
        
        for (i in 0 until multimediaArray.length()) {
            val mediaObj = multimediaArray.getJSONObject(i)
            multimediaList.add(
                Multimedia(
                    type = mediaObj.getString("tipo"),
                    resource = mediaObj.getString("recurso")
                )
            )
        }
        
        // Get first image from array as the main imageUrl
        val imageUrl = multimediaList.firstOrNull { it.type == "imagen" }?.resource ?: ""
        val imageName = imageUrl.substringBeforeLast(".").takeIf { it.isNotEmpty() }
        
        // Lookup category name from categoriaId
        val categoryId = jsonObject.getString("categoriaId")
        val categoryName = categoriesMap?.get(categoryId) ?: categoryId
        
        // Lookup organizer info from organizadorId
        val organizadorId = jsonObject.getString("organizadorId")
        val organizer = organizersMap?.get(organizadorId)
        val organizerName = organizer?.name ?: ""
        val organizerContact = organizer?.contact ?: ""
        
        // Parse location
        val lugar = jsonObject.getJSONObject("lugar")
        val coordenadas = lugar.getJSONObject("coordenadas")
        
        return Event(
            id = jsonObject.getString("id"),
            title = jsonObject.getString("titulo"),
            date = jsonObject.getString("inicio").substring(0, 10),
            startTime = jsonObject.getString("inicio").substring(11, 16),
            endTime = jsonObject.getString("fin").substring(11, 16),
            location = lugar.getString("nombre"),
            description = jsonObject.optString("descripcion", ""),
            imageUrl = imageUrl,
            imageName = imageName,
            multimedia = multimediaList,
            categoryId = categoryId,
            categoryName = categoryName,
            organizerName = organizerName,
            organizerContact = organizerContact,
            latitude = coordenadas.getDouble("lat"),
            longitude = coordenadas.getDouble("lng")
        )
    }
    
    /**
     * Clear cached data
     */
    fun clear() {
        jsonData = null
        eventsCache = null
        categoriesCache = null
        categoriesMap = null
        organizersMap = null
    }
}
