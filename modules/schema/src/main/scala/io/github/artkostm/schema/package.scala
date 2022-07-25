package io.github.artkostm

import higherkindness.droste.data.{AttrF, Fix}

package object schema:
  type FSchema = Fix[SchemaF]
  type EnvT[A] = AttrF[DataF, FSchema, A]
