package org.jprolog.library;

import org.junit.jupiter.api.Test;
import org.jprolog.test.Given;
import org.jprolog.test.PrologTest;

import static org.jprolog.test.Matchers.*;

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
                .and("add_age_x(Name, C1, C2, C3, Vp, Vn) :- age(Name, Age), Vn is (Age * C1 + Vp * C2) + C3.")
                .and("add_age_x(Name, C1, C2, Vp, Vn) :- add_age_x(Name, C1, C2, 0, Vp, Vn).")
                .and("add_age_x(Name, C1, Vp, Vn) :- add_age_x(Name, C1, 1, 0, Vp, Vn).")
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
                .assertSuccess()
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
                .variable("Out", isInteger(23))
            ;
        given()
                .when("?- foldl(_, [], -1, -1).")
                .assertSuccess();
        given()
                .when("?- foldl(add_age, [john, james, peter, mark, jayne], -1, Out).")
                .assertSuccess()
                .variable("Out", isInteger(200))
            ;
    }

    @Test
    public void testScanL4() {
        // scanl expects a goal/predicate of form goal(+Left, +Right, -Result)
        given()
                .when("?- add_age(james, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(23))
        ;
        given()
                .when("?- scanl(_, [], -1, [-1]).")
                .assertSuccess();
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

    @Test
    public void testFoldScanL5() {
        // foldl and scanl with 2 lists (name, scale)
        given()
                .when("?- add_age_x(james, 10, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(203))
        ;
        given()
                .when("?- scanl(_, [], [], -1, [-1]).")
                .assertSuccess();
        given()
                .when("?- foldl(_, [], [], -1, -1).")
                .assertSuccess();
        given()
                .when("?- scanl(add_age_x, [john, james, peter, mark, jayne], [2, 3, 4, 5, 6], -1, Out).")
                .assertSuccess()
                .variable("Out",
                        isList(
                                isInteger(-1), // V0
                                isInteger(125), // -1 + 63*2 = 125
                                isInteger(185), // 125 + 20*3 = 185
                                isInteger(237), // 185 + 13*4 = 237
                                isInteger(587), // 237 + 70*5 = 587
                                isInteger(797)  // 587 + 35*6 = 797
                        ))
        ;
        given()
                .when("?- foldl(add_age_x, [john, james, peter, mark, jayne], [2, 3, 4, 5, 6], -1, Out).")
                .assertSuccess()
                .variable("Out", isInteger(797))
        ;
    }

    @Test
    public void testFoldScanL6() {
        // foldl and scanl with 3 lists (name, scale1, scale2)
        given()
                .when("?- add_age_x(james, 10, 5, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(215))
        ;
        given()
                .when("?- scanl(_, [], [], [], -1, [-1]).")
                .assertSuccess();
        given()
                .when("?- foldl(_, [], [], [], -1, -1).")
                .assertSuccess();
        given()
                .when("?- scanl(add_age_x, [john, james, peter, mark, jayne], [2, 3, 4, 5, 6], [3, 2, 1, 0, -1], -1, Out).")
                .assertSuccess()
                .variable("Out",
                        isList(
                                isInteger(-1), // V0
                                isInteger(123), // -1*3 + 63*2 = 123
                                isInteger(306), // 123*2 + 20*3 = 306
                                isInteger(358), // 306*1 + 13*4 = 358
                                isInteger(350), // 237*0 + 70*5 = 350
                                isInteger(-140)  // 350*-1 + 35*6 = -140
                        ))
        ;
        given()
                .when("?- foldl(add_age_x, [john, james, peter, mark, jayne], [2, 3, 4, 5, 6], [3, 2, 1, 0, -1], -1, Out).")
                .assertSuccess()
                .variable("Out", isInteger(-140))
        ;
    }

    @Test
    public void testFoldScanL7() {
        // foldl and scanl with 4 lists (name, scale1, scale2, offset)
        given()
                .when("?- add_age_x(james, 10, 5, -15, 3, Out).")
                .assertSuccess()
                .variable("Out", isInteger(200))
        ;
        given()
                .when("?- scanl(_, [], [], [], -1, [-1]).")
                .assertSuccess();
        given()
                .when("?- foldl(_, [], [], [], -1, -1).")
                .assertSuccess();
        given()
                .when("?- scanl(add_age_x, " +
                        "[john, james, peter, mark, jayne], " +
                        "[2, 3, 4, 5, 6], [3, 2, 1, 0, -1], [10, 100, 200, 300, 1000], " +
                        "-1, Out).")
                .assertSuccess()
                .variable("Out",
                        isList(
                                isInteger(-1), // V0
                                isInteger(133), // -1*3 + 63*2 + 10 = 133
                                isInteger(426), // 133*2 + 20*3 + 100 = 426
                                isInteger(678), // 426*1 + 13*4 + 200 = 678
                                isInteger(650), // 678*0 + 70*5 + 300 = 650
                                isInteger(560)  // 650*-1 + 35*6 + 1000 = 560
                        ))
        ;
        given()
                .when("?- foldl(add_age_x, " +
                        "[john, james, peter, mark, jayne], " +
                        "[2, 3, 4, 5, 6], [3, 2, 1, 0, -1], [10, 100, 200, 300, 1000], " +
                        "-1, Out).")
                .assertSuccess()
                .variable("Out", isInteger(560))
        ;
    }
}
