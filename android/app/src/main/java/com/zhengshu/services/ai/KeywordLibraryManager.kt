package com.zhengshu.services.ai

import android.content.Context
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
                val inputStream = context.assets.open("fraud_keywords.json")
                val reader = InputStreamReader(inputStream)
                keywordLibrary = gson.fromJson(reader, KeywordLibrary::class.java)
                reader.close()
                inputStream.close()
            }
            Result.success(keywordLibrary!!)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    suspend fun searchKeywords(text: String): List<KeywordMatch> = withContext(Dispatchers.IO) {
        val matches = mutableListOf<KeywordMatch>()
        
        val library = loadKeywords().getOrNull() ?: return@withContext matches
        
        library.categories.forEach { (categoryId, category) ->
            category.keywords.forEach { keyword ->
                if (text.contains(keyword, ignoreCase = true)) {
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
            val keywords = matches.take(3).joinToString("、") { it.keyword }
            "检测到$category相关内容：$keywords"
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
