% Author: Jamie Hunter, 2019
% Refer to LICENSE.TXT for copyright and license information
%
% Option processing, for compatibility with open/4, write_term/3 etc, which are done internally
%
% The standard syntax Name(Value) and extended syntax '='(Name,Value) (Name=Value) are supported.
%

% These are the legal ways of specifying an option
'$option'(Op, E) :- Op = E.
'$option'(Op, E) :-
        E =.. [(=), Name, Arg],
        Op =.. [Name, Arg].

% select_option/4
% select_option(?Option, +Options, -RestOptions, +Default)
% See also exlude/3 and partition/4
% This is base for all other variants
%
select_option(Op, [], [], D) :-
    ! , nonvar(Op),
    ignore((Op =.. [_, D])) .
select_option(Op, [H|T], X, D) :-
    !, nonvar(Op), nonvar(H),
    '$option'(Op, H) ->
        select_option(Op, T, X, D), ! ;
        select_option(Op, T, Q, D), X=[H|Q], ! .

% select_option/3
% select_option(Option, Options, RestOptions)
% See also exclude/3
%
select_option(Op, L, R) :- select_option(Op, L, R, _) .

% option/2
% option(?Option, +Options)
%
option(Op, L) :- select_option(Op, L, _, _).

% option/3
% option(?Option, +Options, +Default)
%
option(Op, L, D) :- select_option(Op, L, _, D).
