package com.animalvision.app.ui

/**
 * One practical "try this" per species: where to point the camera so the modeled difference
 * is actually visible. Many differences are real but subtle in an ordinary room (a dog's
 * red/green collapse needs red and green in view), so the tip tells people where to look
 * instead of the app exaggerating the model.
 */
fun tryTip(id: String): String? = when (id) {
    "human" -> "Your baseline. Pick an animal, then press and hold the view to compare."
    "dog" -> "Point at red and green together — fruit, flowers, a toy on grass. To a dog they look alike."
    "cat" -> "Try red against green, then a dim room: cats trade color and detail for low-light vision."
    "horse" -> "Look just below the nose: a horse can't see there. Red and green merge too."
    "eagle" -> "Zoom to 4×: an eagle still resolves finer detail than you do at any distance."
    "bee" -> "Point at red flowers: to a bee, red is dark. Zoom in to see how coarse bee vision is."
    "snake" -> "The thermal look is simulated from brightness — warm-looking areas are just brighter."
    "pigeon", "chicken" ->
        "Birds see more colors than you. A screen can't show the extra ones, so this view looks close to yours."
    "butterfly" -> "Butterflies see ultraviolet; the UV channel here is a labeled proxy from blue."
    "fly" -> "Zoom to 4× to see how little detail a fruit fly resolves."
    "octopus" -> "No color at all. Watch bright glare off water or glass — octopuses read polarized light."
    "mantisshrimp" -> "Aim at shiny water or glass: that glare is where polarized light is strongest."
    "dolphin" -> "Grey and soft: one cone type and modest acuity. Shiny surfaces still stand out."
    else -> null
}
