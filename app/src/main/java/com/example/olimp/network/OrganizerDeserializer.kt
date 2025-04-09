package com.example.olimp.network

import com.example.olimp.data.models.Organizer
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class OrganizerDeserializer : JsonDeserializer<Organizer> {
    override fun deserialize(
        json: JsonElement?,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Organizer {
        // Если json null или пустой, возвращаем Organizer с минимальными данными
        if (json == null || json.isJsonNull) {
            throw IllegalArgumentException("Organizer JSON cannot be null")
        }

        // 1) Если organizer приходит просто числом (ID)
        if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            return Organizer.fromId(json.asInt)
        }

        // 2) Если organizer приходит объектом, парсим поля вручную
        val jsonObj = json.asJsonObject

        // Безопасно извлекаем поля, проверяя на null
        val id = jsonObj.get("id")?.asInt
        val username = jsonObj.get("username")?.takeIf { !it.isJsonNull }?.asString
        val email = jsonObj.get("email")?.takeIf { !it.isJsonNull }?.asString
        val role = jsonObj.get("role")?.takeIf { !it.isJsonNull }?.asString
        val avatar = jsonObj.get("avatar")?.takeIf { !it.isJsonNull }?.asString
        val bio = jsonObj.get("bio")?.takeIf { !it.isJsonNull }?.asString
        val createdAt = jsonObj.get("created_at")?.takeIf { !it.isJsonNull }?.asString
        val updatedAt = jsonObj.get("updated_at")?.takeIf { !it.isJsonNull }?.asString

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