/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.saff.befuzz

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val json = Json { prettyPrint = true }

@Serializable
data class BehaviorProfile(val adventures: List<AdventureLog>) {
  fun asJson(): Any {
    return json.encodeToString(this)
  }
}

fun behaviorProfile(fates: Fates, fn: Adventure.() -> String): BehaviorProfile {
  return BehaviorProfile(fates.allFates().map {
    Adventure(it).run {
      extractLog(
        try {
          fn()
        } catch (e: AssumptionViolatedException) {
          "Violated assumption: ${e.message}"
        }
      )
    }
  }.toList())
}