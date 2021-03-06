package io.circe.optics

import monocle.{ Lens, Optional, Prism, Traversal }
import monocle.function.{ At, Each, FilterIndex, Index }
import monocle.law.{ LensLaws, OptionalLaws, PrismLaws, TraversalLaws }
import monocle.law.discipline.isEqToProp
import org.scalacheck.{ Arbitrary, Prop, Shrink }
import org.typelevel.discipline.Laws
import scalaz.Equal
import scalaz.std.list._
import scalaz.std.option._

/**
 * We use our own implementations because Monocle's don't (currently) use non-default `Shrink`
 * instances. If Monocle changes this we will remove this code.
 */
object LawsTests extends Laws {
  def atTests[S: Arbitrary: Shrink: Equal, I: Arbitrary: Shrink, A: Arbitrary: Equal](implicit
    evAt: At[S, I, A],
    arbAA: Arbitrary[A => A]
  ): RuleSet = {
    new SimpleRuleSet("At", lensTests(At.at(_: I)).props: _*)
  }

  def indexTests[S: Arbitrary: Shrink: Equal, I: Arbitrary: Shrink, A: Arbitrary: Shrink: Equal](implicit
    evIndex: Index[S, I, A],
    arbAA: Arbitrary[A => A]
  ): RuleSet = new SimpleRuleSet("Index", optionalTests(Index.index(_ : I)).props: _*)

  def filterIndexTests[S: Arbitrary: Shrink: Equal, I, A: Arbitrary: Shrink: Equal](implicit
    evFilterIndex: FilterIndex[S, I, A],
    arbAA: Arbitrary[A => A], arbIB: Arbitrary[I => Boolean]
  ): RuleSet = new SimpleRuleSet("FilterIndex", traversalTests(FilterIndex.filterIndex(_: I => Boolean)).props: _*)

  def eachTests[S: Arbitrary: Shrink: Equal, A: Arbitrary: Shrink: Equal](implicit
    evEach: Each[S, A],
    arbAA: Arbitrary[A => A]
  ): RuleSet = new SimpleRuleSet("Each", traversalTests(Each.each[S, A]).props: _*)

  def lensTests[S: Arbitrary: Equal, A: Arbitrary: Shrink: Equal, I: Arbitrary: Shrink](f: I => Lens[S, A])(implicit
    arbAA: Arbitrary[A => A]
  ): RuleSet = {
    def laws(i: I) = LensLaws(f(i))

    new SimpleRuleSet(
      "Lens",
      "set what you get"  -> Prop.forAll((s: S, i: I) => laws(i).getSet(s)),
      "get what you set"  -> Prop.forAll((s: S, a: A, i: I) => laws(i).setGet(s, a)),
      "set idempotent"    -> Prop.forAll((s: S, a: A, i: I) => laws(i).setIdempotent(s, a)),
      "modify id = id"    -> Prop.forAll((s: S, i: I) => laws(i).modifyIdentity(s)),
      "compose modify"    -> Prop.forAll((s: S, g: A => A, h: A => A, i: I) => laws(i).composeModify(s, g, h)),
      "consistent set with modify"      -> Prop.forAll((s: S, a: A, i: I) => laws(i).consistentSetModify(s, a)),
      "consistent modify with modifyId" ->
        Prop.forAll((s: S, g: A => A, i: I) => laws(i).consistentModifyModifyId(s, g)),
      "consistent get with modifyId"    -> Prop.forAll((s: S, i: I) => laws(i).consistentGetModifyId(s))
    )
  }

  def optionalTests[S: Arbitrary: Shrink: Equal, A: Arbitrary: Shrink: Equal, I: Arbitrary: Shrink](
    f: I => Optional[S, A]
  )(implicit arbAA: Arbitrary[A => A]): RuleSet = {
    def laws(i: I) = OptionalLaws(f(i))

    new SimpleRuleSet(
      "Optional",
      "set what you get"  -> Prop.forAll((s: S, i: I) => laws(i).getOptionSet(s)),
      "get what you set"  -> Prop.forAll((s: S, a: A, i: I) => laws(i).setGetOption(s, a)),
      "set idempotent"    -> Prop.forAll((s: S, a: A, i: I) => laws(i).setIdempotent(s, a)),
      "modify id = id"    -> Prop.forAll((s: S, i: I) => laws(i).modifyIdentity(s)),
      "compose modify"    -> Prop.forAll((s: S, g: A => A, h: A => A, i: I) => laws(i).composeModify(s, g, h)),
      "consistent set with modify"         -> Prop.forAll((s: S, a: A, i: I) => laws(i).consistentSetModify(s, a)),
      "consistent modify with modifyId"    -> Prop.forAll((s: S, g: A => A, i: I) => laws(i).consistentModifyModifyId(s, g)),
      "consistent getOption with modifyId" -> Prop.forAll((s: S, i: I) => laws(i).consistentGetOptionModifyId(s))
    )
  }

  def prismTests[S: Arbitrary: Shrink: Equal, A: Arbitrary: Shrink: Equal](prism: Prism[S, A])(implicit
    arbAA: Arbitrary[A => A]
  ): RuleSet = {
    val laws: PrismLaws[S, A] = new PrismLaws(prism)

    new SimpleRuleSet(
      "Prism",
      "partial round trip one way" -> Prop.forAll((s: S) => laws.partialRoundTripOneWay(s)),
      "round trip other way" -> Prop.forAll((a: A) => laws.roundTripOtherWay(a)),
      "modify id = id"       -> Prop.forAll((s: S) => laws.modifyIdentity(s)),
      "compose modify"       -> Prop.forAll((s: S, f: A => A, g: A => A) => laws.composeModify(s, f, g)),
      "consistent set with modify"         -> Prop.forAll((s: S, a: A) => laws.consistentSetModify(s, a)),
      "consistent modify with modifyId"    -> Prop.forAll((s: S, g: A => A) => laws.consistentModifyModifyId(s, g)),
      "consistent getOption with modifyId" -> Prop.forAll((s: S) => laws.consistentGetOptionModifyId(s))
    )
  }

  def traversalTests[S: Arbitrary: Shrink: Equal, A: Arbitrary: Shrink: Equal](traversal: Traversal[S, A])(implicit
    arbAA: Arbitrary[A => A]
  ): RuleSet = traversalTests[S, A, Unit](_ => traversal)

  def traversalTests[S: Arbitrary: Shrink: Equal, A: Arbitrary: Shrink: Equal, I: Arbitrary: Shrink](
    f: I => Traversal[S, A]
  )(implicit arbAA: Arbitrary[A => A]): RuleSet = {
    def laws(i: I): TraversalLaws[S, A] = new TraversalLaws(f(i))

    new SimpleRuleSet("Traversal",
      "headOption"        -> Prop.forAll((s: S, i: I) => laws(i).headOption(s)),
      "get what you set"  -> Prop.forAll((s: S, f: A => A, i: I) => laws(i).modifyGetAll(s, f)),
      "set idempotent"    -> Prop.forAll((s: S, a: A, i: I) => laws(i).setIdempotent(s, a)),
      "modify id = id"    -> Prop.forAll((s: S, i: I) => laws(i).modifyIdentity(s)),
      "compose modify"    -> Prop.forAll((s: S, f: A => A, g: A => A, i: I) => laws(i).composeModify(s, f, g))
    )
  }
}