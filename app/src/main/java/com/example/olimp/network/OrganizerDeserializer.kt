package com.example.olimp.network

import com.example.olimp.data.models.Organizer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class OrganizerDeserializer : JsonDeserializer<Organizer> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type,
        context: JsonDeserializationContext
    ): Organizer {
        // 1) Если organizer приходит просто числом (ID):
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            return Organizer.fromId(json.asInt)
        }

        // 2) Если organizer приходит объектом, парсим поля вручную
        val jsonObj = json.asJsonObject

        // Предположим, что поля называются "id" и "username"
        val id = jsonObj.get("id")?.asInt
        val username = jsonObj.get("username")?.asString
        val email = jsonObj.get("email")?.asString
        val role = jsonObj.get("role")?.asString
        val avatar = jsonObj.get("avatar")?.asString
        val bio = jsonObj.get("bio")?.asString
        val createdAt = jsonObj.get("created_at")?.asString
        val updatedAt = jsonObj.get("updated_at")?.asString

        return Organizer(
            id = id,
            username = username,
            email = email,
            role = role,
            avatar = avatar,
            bio = bio,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}
