package com.garfiec.util

class Environment {
    val databaseUrl: String = System.getenv("DATABASE_URL") ?: throw IllegalArgumentException("DATABASE_URL not set")
    val databaseUsername: String = System.getenv("DATABASE_USERNAME") ?: throw IllegalArgumentException("DATABASE_USERNAME not set")
    val databasePassword: String = System.getenv("DATABASE_PASSWORD") ?: throw IllegalArgumentException("DATABASE_PASSWORD not set")
    val databasePort: String = System.getenv("DATABASE_PORT") ?: throw IllegalArgumentException("DATABASE_PORT not set")
    val minioUrl: String = System.getenv("MINIO_URL") ?: throw IllegalArgumentException("MINIO_URL not set")
    val minioAccessKey: String = System.getenv("MINIO_ACCESS_KEY") ?: throw IllegalArgumentException("MINIO_ACCESS_KEY not set")
    val minioSecretKey: String = System.getenv("MINIO_SECRET_KEY") ?: throw IllegalArgumentException("MINIO_SECRET_KEY not set")
    val openAiKey: String = System.getenv("OPENAI_KEY") ?: throw IllegalArgumentException("OPENAI_KEY not set")
}
