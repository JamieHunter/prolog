% Author: Jamie Hunter, 2019
% Refer to LICENSE.TXT for copyright and license information
%
% Script to implement consult.
% TODO: This is work in progress
%

% Consult with single entry list will consult the single file in the list
consult([H]) :- consult(H).

% Consult with a list, will visit each file in the list
consult([H|T]) :- consult(H), !, consult(T).

% Consult with a single file
consult(File) :-
    % TODO: Auto-append ".pl"
    % TODO: Handle "user"

    open(File, read, Stream),
    '$consult_stream'(Stream),
    close(Stream).

% internal - given a stream, consult the stream until EOF
'$consult_stream'(Stream) :-
    '$consult_next'(Stream, Disp),!,
    '$consult_and_then'(Stream, Disp).

% internal - consult next sentence.
'$consult_next'(Stream, Disp) :-
    '$consult_prompt'(Stream),
    read(Stream, T),
    '$consult_handle'(T, Disp).

% internal - handle end-of-file (success)
'$consult_handle'(end_of_file, end_of_file).

% internal - handle directive (call Goal)
'$consult_handle'((:-)(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'directive'.

% internal - handle query (call Goal)
'$consult_handle'((?-)(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'query'.

% internal - everything else is fact or clause
'$consult_handle'(X, Disp) :-
    % TODO: Handle reconsultation
    assertz(X), Disp = 'assert'.

% internal - call goal, with fast fail
'$consult_goal'(Goal) :-
    call(Goal),! ;
    % TODO: Standard error to throw here?
    throw(failed_goal(Goal)).

% internal - finish if EOF
'$consult_and_then'(Stream, end_of_file).

% internal - keep going if not end of file (tail-call recursion)
'$consult_and_then'(Stream, X) :- !, '$consult_stream'(Stream).

% [...] is shortcut for consult
[H|T] :- consult([H|T]).

% :- Make private ...
