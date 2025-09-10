package com.example.gencont_app.aiintegration.service_video.model

import android.content.Context
import com.example.gencont_app.configDB.sqlite.data.Cours
import com.example.gencont_app.configDB.sqlite.data.Prompt
import com.example.gencont_app.configDB.sqlite.data.Section
import com.example.gencont_app.configDB.sqlite.database.AppDatabase

data class VideoQueryRequest(
    val cours: Cours,
    val sections: List<Section>,
    val context: Context
) {
    private val db = AppDatabase.getInstance(context)
    private val daoPrompt = db.promptDao()

    suspend fun getPrompt(): Prompt? {
        return cours.promptId?.let { daoPrompt.getPromptById(it) }
    }

    suspend fun toPrompt(): String {
        val prompt = getPrompt()
        val sb = StringBuilder()

        sb.append("Je souhaite obtenir une ressource pédagogique vidéo pour chaque section du cours suivant.\n")
        sb.append("Pour chaque section, fournis :\n")
        sb.append("- Un lien vers une vidéo YouTube explicative (durée max 20 min)\n")
        sb.append("- Une brève description de cette vidéo (1 ou 2 phrases max)\n")
        sb.append("- La vidéo doit être disponible publiquement, sans restrictions géographiques ni de connexion\n")
        sb.append("- Ne propose aucune vidéo marquée comme « non disponible » ou « supprimée »\n")
        sb.append("- Vérifie que le lien YouTube fonctionne au moment de la génération\n")
        sb.append("- Si aucune vidéo valide n’est trouvée pour une section, mets \"null\" pour le champ \"video\"\n\n")

        sb.append("### Informations générales du cours ###\n")
        sb.append("- Titre : \"${prompt?.coursName ?: cours.titre ?: "Non spécifié"}\"\n")
        sb.append("- Description : \"${prompt?.description ?: cours.description ?: "Aucune description fournie"}\"\n")
        sb.append("- Niveau : \"${prompt?.niveau ?: "Non spécifié"}\"\n")
        sb.append("- Langue : \"${prompt?.langue ?: "Non spécifiée"}\"\n\n")

        sb.append("### Sections à traiter ###\n\n")
        sections.forEachIndexed { index, section ->
            sb.append("Section ${index + 1} :\n")
            sb.append("- Titre : \"${section.titre}\"\n")
            sb.append("- Contenu : \"${section.contenu.take(300)}\"\n")
            if (!section.exemple.isNullOrBlank()) {
                sb.append("- Exemple : \"${section.exemple.take(150)}\"\n")
            }
            sb.append("\n")
        }

        sb.append("### Format de réponse JSON attendu (sans texte autour) ###\n")
        sb.append("{\n")
        sb.append("  \"sections\": [\n")
        sections.forEachIndexed { index, section ->
            sb.append("    {\n")
            sb.append("      \"section\": ${index + 1},\n")
            sb.append("      \"titre\": \"${section.titre}\",\n")
            sb.append("      \"video\": \"lien vidéo YouTube ou null\",\n")
            sb.append("      \"description\": \"brève description de la vidéo ou null\"\n")
            sb.append("    }")
            if (index != sections.size - 1) sb.append(",")
            sb.append("\n")
        }
        sb.append("  ]\n")
        sb.append("}\n")

        return sb.toString()
    }



    suspend fun toPromptForSection(section: Section, index: Int): String {
        val prompt = getPrompt()
        val sb = StringBuilder()

        sb.append("Je souhaite une vidéo pédagogique pour la section suivante d’un cours.\n")
        sb.append("- Fournis un lien YouTube explicatif (durée max 20 min)\n")
        sb.append("- Ajoute une brève description de la vidéo\n")
        sb.append("- Assure-toi que le lien est valide, public, non restreint, et fonctionne\n")
        sb.append("- Si aucune vidéo n'est valide, indique \"null\"\n\n")

        sb.append("### Détails du cours ###\n")
        sb.append("- Titre : \"${prompt?.coursName ?: cours.titre ?: "Non spécifié"}\"\n")
        sb.append("- Niveau : \"${prompt?.niveau ?: "Non spécifié"}\"\n")
        sb.append("- Langue : \"${prompt?.langue ?: "Non spécifiée"}\"\n\n")

        sb.append("### Section ###\n")
        sb.append("Section ${index + 1} :\n")
        sb.append("- Titre : \"${section.titre}\"\n")
        sb.append("- Contenu : \"${section.contenu.take(300)}\"\n")
        if (!section.exemple.isNullOrBlank()) {
            sb.append("- Exemple : \"${section.exemple.take(150)}\"\n")
        }

        sb.append("\n### Format de réponse attendu ###\n")
        sb.append("{\n")
        sb.append("  \"video\": \"lien vidéo YouTube ou null\",\n")
        sb.append("  \"description\": \"brève description ou null\"\n")
        sb.append("}\n")

        return sb.toString()
    }

}