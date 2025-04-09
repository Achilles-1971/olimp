package com.example.olimp.network

import com.example.olimp.data.models.Organizer
import com.google.gson.*
import java.lang.reflect.Type

class OrganizerAdapter : JsonDeserializer<Organizer> {
    override fun deserialize(json: JsonElement, typeOfT: Type, context: JsonDeserializationContext): Organizer {
        return if (json.isJsonPrimitive && json.asJsonPrimitive.isNumber) {
            // Если пришёл просто id
            Organizer.fromId(json.asInt)
        } else {
            // Если пришёл полноценный объект
            context.deserialize(json, Organizer::class.java)
        }
    }
}