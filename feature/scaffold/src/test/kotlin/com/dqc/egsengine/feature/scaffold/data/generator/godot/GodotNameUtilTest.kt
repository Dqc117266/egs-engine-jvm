package com.dqc.egsengine.feature.scaffold.data.generator.godot

import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should throw`
import org.junit.jupiter.api.Test

class GodotNameUtilTest {
    @Test
    fun `valid names pass validation`() {
        listOf("slime", "BossArena", "boss_arena", "Fire2", "a", "A_b1_c")
            .forEach { GodotNameUtil.isValidEntityName(it) `should be equal to` true }
    }

    @Test
    fun `rejects names starting with digit or containing invalid chars`() {
        listOf("1slime", "", "_boss", "boss-arena", "boss arena", "ねこ", "boss!")
            .forEach { GodotNameUtil.isValidEntityName(it) `should be equal to` false }
    }

    @Test
    fun `requireValidEntityName throws for invalid input`() {
        val validate = { GodotNameUtil.requireValidEntityName("1bad") }
        validate `should throw` IllegalArgumentException::class
    }

    @Test
    fun `toSnakeCase collapses separators and lowercases`() {
        GodotNameUtil.toSnakeCase("boss_arena") `should be equal to` "boss_arena"
        GodotNameUtil.toSnakeCase("BossArena") `should be equal to` "boss_arena"
        GodotNameUtil.toSnakeCase("Fire__Ball") `should be equal to` "fire_ball"
        GodotNameUtil.toSnakeCase("slime") `should be equal to` "slime"
        GodotNameUtil.toSnakeCase("HP_MAX") `should be equal to` "hp_max"
        GodotNameUtil.toSnakeCase("fireBall2") `should be equal to` "fire_ball2"
    }

    @Test
    fun `toPascalCase capitalizes each segment`() {
        GodotNameUtil.toPascalCase("boss_arena") `should be equal to` "BossArena"
        GodotNameUtil.toPascalCase("slime") `should be equal to` "Slime"
        GodotNameUtil.toPascalCase("Fire__Ball") `should be equal to` "FireBall"
        GodotNameUtil.toPascalCase("BossArena") `should be equal to` "BossArena"
        GodotNameUtil.toPascalCase("HP_MAX") `should be equal to` "HpMax"
    }
}
