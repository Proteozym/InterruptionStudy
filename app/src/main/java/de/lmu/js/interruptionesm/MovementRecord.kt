package de.lmu.js.interruptionesm

class MovementRecord(newMov: Movement, newConf: Int) {
    enum class Movement() {
        IN_VEHICLE, ON_BICYCLE, RUNNING, STILL, WALKING, NONE
    }
    lateinit var movement: Movement
    var confidence: Int = 0

    init {
        movement = newMov
        confidence = newConf
    }
}

