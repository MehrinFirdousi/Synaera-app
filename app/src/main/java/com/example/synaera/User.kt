package com.example.synaera

class User(var id : Int, var email : String, var name : String, var password : String) : java.io.Serializable {
    override fun toString(): String {
        return "User($id, $email, $name, $password)"
    }
}