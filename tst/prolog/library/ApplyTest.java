package prolog.library;

import org.junit.Ignore;
import org.junit.Test;
import prolog.test.Given;
import prolog.test.PrologTest;

import static prolog.test.Matchers.isAtom;
import static prolog.test.Matchers.isInteger;
import static prolog.test.Matchers.isList;

/**
 * Tests for apply.pl (Apply library), not to be confused with the apply/2 predicate.
 */
public class ApplyTest {

    protected Given given() {
        return PrologTest
                .given("old(X) :- age(X,Y), Y > 30 .")
                .and("record(Age, X) :- age(X,Y), (Y > Age -> assertz(isold(X)); assertz(isyoung(X))) .")
                .and("record(X) :- record(30, X) .")
                .and("record(A, B, X) :- Age is A+B, record(Age, X) .")
                .and("add_age(Name, Vp, Vn) :- age(Name, Age), Vn is Age + Vp .")
                .and("age(john, 63).")
                .and("age(peter, 13).")
                .and("age(james, 20).")
                .and("age(jayne, 35).")
                .and("age(mark, 70).")
                .and("age(suzie, 80).")
                ;

    }

    @Test
    public void testInclude() {
        given()
                .when("?- include(old, [john, peter, james, suzie], [john, suzie]).")
                .assertSuccess()
            ;
        given()
                .when("?- include(old, [mark, suzie, james, suzie], Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("mark"), isAtom("suzie"), isAtom("suzie")))
            ;
        // Todo backtracking examples - current assumption is that once() operation is assumed
    }

    @Test
    public void testExclude() {
        given()
                .when("?- exclude(old, [john, peter, james, suzie], [peter, james]).")
                .assertSuccess()
        ;
        given()
                .when("?- exclude(old, [peter, suzie, james, peter], Y).")
                .assertSuccess()
                .variable("Y", isList(isAtom("peter"), isAtom("james"), isAtom("peter")))
        ;
        // Todo backtracking examples - current assumption is that once() operation is assumed
    }

    @Test
    public void testParition() {
        given()
                .when("?- partition(old, [john, peter, james, suzie], [john, suzie], [peter, james]).")
                .assertSuccess()
        ;
        given()
                .when("?- partition(old, [peter, suzie, james, jayne], Y, Z).")
                .assertSuccess()
                .variable("Y", isList(isAtom("suzie"), isAtom("jayne")))
                .variable("Z", isList(isAtom("peter"), isAtom("james")))
        ;

        // Todo backtracking examples - current assumption is that once() operation is assumed
    }

    @Test
    public void testMapList2() {
        given()
                .when("?- maplist(old, [john, peter, james, suzie]).")
                .assertFailed()
        ;
        given()
                .when("?- maplist(old, [john, jayne, mark, suzie]).")
                .assertSuccess()
        ;
        given()
                .when("?- maplist(record, [john, peter, james, suzie]).")
                .assertSuccess()
                .andWhen("?- isold(john), isold(suzie), isyoung(peter), isyoung(james).")
                .assertSuccess();
        ;

    }

    @Test
    public void testMapList3() {
        given()
                .when("?- maplist(age, [john, peter, james, suzie], [63, 12, 20, 80]).")
                .assertFailed()
        ;
        given()
                .when("?- maplist(age, [john, peter, james, suzie], [63, 13, 20, 80]).")
                .assertSuccess()
        ;
        given()
                .when("?- maplist(record, [70, 10, 15, 90], [john, peter, james, suzie]).")
                .assertSuccess()
                .andWhen("?- isyoung(john).")
                .assertSuccess()
                .andWhen("?- isold(peter).")
                .assertSuccess()
                .andWhen("?- isold(james).")
                .assertSuccess()
                .andWhen("?- isyoung(suzie).")
                .assertSuccess()
        ;

    }

    @Test
    public void testMapList4() {
        given()
                .when("?- maplist(record, [30, 20, 7, 45], [40, -10, 8, 45], [john, peter, james, suzie]).")
                .assertSuccess()
                .andWhen("?- isyoung(john).")
                .assertSuccess()
                .andWhen("?- isold(peter).")
                .assertSuccess()
                .andWhen("?- isold(james).")
                .assertSuccess()
                .andWhen("?- isyoung(suzie).")
                .assertSuccess()
        ;
    }

    @Test
    public void testConvList() {
        given()
                .when("?- convlist(age, [john, jack, peter, smith, mark], [63, 13, 70]).")
                .assertSuccess()
            ;
        given()
                .when("?- convlist(age, [john, jack, peter, smith, mark], Z).")
                .assertSuccess()
                .variable("Z", isList(isInteger(63), isInteger(13), isInteger(70)))
            ;
    }

    @Test
    public void testFoldL4() {
        // foldl expects a goal/predicate of form goal(+Left, +Right, -Result)
        given()
                .when("?- add_age(james, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(23));
            ;
        given()
                .when("?- foldl(_, [], -1, -1).");
        given()
                .when("?- foldl(add_age, [john, james, peter, mark, jayne], -1, Out).")
                .assertSuccess()
                .variable("Out", isInteger(200))
            ;
    }

    @Test
    @Ignore("scanl is currently broken")
    public void testScanL4() {
        // scanl expects a goal/predicate of form goal(+Left, +Right, -Result)
        given()
                .when("?- add_age(james, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(23));
        ;
        given()
                .when("?- scanl(_, [], -1, [-1]).");
        given()
                .when("?- scanl(add_age, [john, james, peter, mark, jayne], -1, Out).")
                .assertSuccess()
                .variable("Out",
                        isList(
                                isInteger(-1), // V0
                                isInteger(62), // -1 + 63 = 62
                                isInteger(82), // 62 + 20 = 82
                                isInteger(95), // 82 + 13 = 95
                                isInteger(165), // 95 + 70 = 165
                                isInteger(200)  // 165 + 35 = 200
                        ))
        ;
    }
}
