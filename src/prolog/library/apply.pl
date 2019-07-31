% Apply predicates across lists etc
% This is used by consult, and is a primitive demand-load library
% All of these use the call/2 (etc) variant of apply/2


%
% include/3
% include(:Goal, +List1, ?List2)
%

include(Goal, [], []) :- !.
include(Goal, [H|T], X) :-
    call(Goal, H) ->
        include(Goal, T, Q), X=[H|Q], ! ;
        include(Goal, T, X), ! .

%
% exclude/3
% exclude(:Goal, +List1, ?List2)
%

exclude(Goal, [], []) :- !.
exclude(Goal, [H|T], X) :-
    call(Goal, H) ->
        exclude(Goal, T, X), ! ;
        exclude(Goal, T, Q), X=[H|Q], ! .

%
% partition/4
% partition(:Pred, +List, ?Included, ?Excluded)
%

partition(Pred, [], [], []) :- !.
partition(Pred, [H|T], X, Y) :-
    call(Pred, H) ->
        partition(Pred, T, P, Q), X=[H|P], Y=Q, ! ;
        partition(Pred, T, P, Q), X=P, Y=[H|Q], ! .

%
% TODO: partition/5
% partition(:Pred, +List, ?Less, ?Equal, ?Greater)
%

%
% maplist/2
% maplist(:Goal, +List)
%

maplist(Goal, []) :- !.
maplist(Goal, [H|T]) :-
    call(Goal, H), !, maplist(Goal, T).

%
% maplist/3
% maplist(:Goal, +List1, +List2)
%

maplist(Goal, [], []) :- !.
maplist(Goal, [H1|T1], [H2|T2]) :-
    call(Goal, H1, H2), !, maplist(Goal, T1, T2).

%
% maplist/4
% maplist(:Goal, +List1, +List2, +List3)
%

maplist(Goal, [], [], []) :- !.
maplist(Goal, [H1|T1], [H2|T2], [H3|T3]) :-
    call(Goal, H1, H2, H3), !, maplist(Goal, T1, T2, T3).

% convlist/3
% convlist(:Goal, +ListIn, -ListOut)
%

convlist(Goal, [], []) :- !.
convlist(Goal, [H|T], Q) :-
    call(Goal, H, R) ->
        convlist(Goal, T, S), Q=[R|S], ! ;
        convlist(Goal, T, S), Q=S, !.

%
% foldl/4
% foldl(:Goal, +List1, +V0, -V)
%

foldl(Goal, [], V, V) :- !.
foldl(Goal, [H|T], V0, V) :-
    call(Goal, H, V0, Vh), !,
    foldl(Goal, T, Vh, V).

% foldl/5
% foldl/6
% foldl/7

%
% scanl/4
% scanl(:Goal, +List1, +V0, -Vs)
%

scanl(Goal, [], V, [V]) :- !.
scanl(Goal, [H|T], V0, [V0|Vt]) :-
    call(Goal, H, V0, Vh), !,
    scanl(Goal, T, Vh, Vt), !
    % TODO: currently not working (interpreter bug?)
    .


% scanl/5
% scanl/6
% scanl/7
