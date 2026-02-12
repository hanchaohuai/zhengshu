package com.zhengshu.services.ai

import android.content.Context
import android.util.Log
import com.google.gson.Gson
import com.zhengshu.data.model.RiskLevel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStreamReader

data class KeywordLibrary(
    val version: String,
    val last_updated: String,
    val categories: Map<String, KeywordCategory>
)

data class KeywordCategory(
    val name: String,
    val risk_level: String,
    val keywords: List<String>
)

class KeywordLibraryManager(private val context: Context) {
    
    private val gson = Gson()
    private var keywordLibrary: KeywordLibrary? = null
    
    suspend fun loadKeywords(): Result<KeywordLibrary> = withContext(Dispatchers.IO) {
        try {
            if (keywordLibrary == null) {
                Log.d("KeywordLibraryManager", "Loading fraud_keywords.json from assets")
                val inputStream = context.assets.open("fraud_keywords.json")
                val reader = InputStreamReader(inputStream)
                keywordLibrary = gson.fromJson(reader, KeywordLibrary::class.java)
                reader.close()
                inputStream.close()
                Log.d("KeywordLibraryManager", "Keywords loaded successfully: ${keywordLibrary?.categories?.size} categories")
                keywordLibrary?.categories?.forEach { (id, category) ->
                    Log.d("KeywordLibraryManager", "Category '$id' (${category.name}): ${category.keywords.size} keywords, risk level: ${category.risk_level}")
                }
            }
            Result.success(keywordLibrary!!)
        } catch (e: Exception) {
            Log.e("KeywordLibraryManager", "Failed to load keywords: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    suspend fun searchKeywords(text: String): List<KeywordMatch> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<KeywordMatch>()
        
        Log.d("KeywordLibraryManager", "Searching keywords in text: ${text.take(100)}")
        val library = loadKeywords().getOrNull() ?: return@withContext matches
        
        library.categories.forEach { (categoryId, category) ->
            category.keywords.forEach { keyword ->
                if (text.contains(keyword, ignoreCase = true)) {
                    Log.d("KeywordLibraryManager", "Matched keyword: '$keyword' in category: ${category.name}")
                    val riskLevel = when (category.risk_level) {
                        "HIGH" -> RiskLevel.HIGH
                        "MEDIUM" -> RiskLevel.MEDIUM
                        "LOW" -> RiskLevel.LOW
                        else -> RiskLevel.NONE
                    }
                    
                    matches.add(
                        KeywordMatch(
                            keyword = keyword,
                            category = category.name,
                            categoryId = categoryId,
                            riskLevel = riskLevel
                        )
                    )
                }
            }
        }
        
        Log.d("KeywordLibraryManager", "Found ${matches.size} keyword matches")
        matches
    }
    
    suspend fun getHighestRiskLevel(text: String): RiskLevel = withContext(Dispatchers.IO) {
        val matches = searchKeywords(text)
        
        if (matches.isEmpty()) {
            return@withContext RiskLevel.NONE
        }
        
        matches.maxByOrNull { it.riskLevel }?.riskLevel ?: RiskLevel.NONE
    }
    
    suspend fun getRiskReason(text: String): String = withContext(Dispatchers.IO) {
        val matches = searchKeywords(text)
        
        if (matches.isEmpty()) {
            return@withContext ""
        }
        
        val categoryGroups = matches.groupBy { it.category }
        val reasons = categoryGroups.map { (category, matches) ->
            val keywords = matches.take(3).joinToString(", ") { it.keyword }
            "Detected $category related content: $keywords"
        }
        
        reasons.joinToString("；")
    }
    
    suspend fun getAllKeywords(): List<String> = withContext(Dispatchers.IO) {
        val library = loadKeywords().getOrNull() ?: return@withContext emptyList()
        
        library.categories.values.flatMap { it.keywords }
    }
    
    suspend fun getKeywordsByCategory(categoryId: String): List<String> = withContext(Dispatchers.IO) {
        val library = loadKeywords().getOrNull() ?: return@withContext emptyList()
        
        library.categories[categoryId]?.keywords ?: emptyList()
    }
}

data class KeywordMatch(
    val keyword: String,
    val category: String,
    val categoryId: String,
    val riskLevel: RiskLevel
)
