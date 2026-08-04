package com.agrogestao.pro.domain

const val MIN_ACCOUNT_PASSWORD_LENGTH = 8

fun accountPasswordError(password: String, confirmation: String? = null): String? = when {
    password.length < MIN_ACCOUNT_PASSWORD_LENGTH ->
        "A senha precisa ter pelo menos $MIN_ACCOUNT_PASSWORD_LENGTH caracteres."
    confirmation != null && password != confirmation -> "As senhas não são iguais."
    else -> null
}
