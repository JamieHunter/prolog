% Author: Jamie Hunter, 2019
% Refer to LICENSE.TXT for copyright and license information
%
% Script to implement consult.
% TODO: This is work in progress
%

% Consult with single entry list will consult the single file in the list
consult([H]) :- consult(H).

% Consult with a list, will visit each file in the list
consult([H|T]) :- once(consult(H)), consult(T).

% Consult user, special case
consult(user) :-
    '$consult_stream'(user_input).

% Consult with a single file
consult(File) :-
    % TODO: Auto-append ".pl"

    open(File, read, Stream),
    '$consult_stream'(Stream),
    close(Stream).

% internal - given a stream, consult the stream until EOF
'$consult_stream'(Stream) :-
    once('$consult_this'(Stream, Disp)),
    '$consult_next'(Stream, Disp).

% internal - consult next sentence.
'$consult_this'(Stream, Disp) :-
    catch('$consult_read'(Stream, T), error(syntax_error(expected_sentence_error), Cause),
        (
            Stream = user_input ->
                T = end_of_file;
                throw(error(syntax_error(expected_sentence_error), Cause))
        )),
    '$consult_sentence'(T, Disp).

% wrap read with prompts
'$consult_read'(Stream, T) :-
    '$consult_prompt'(Stream),
    catch(read(Stream, T), Error, (
        '$no_prompt'(Stream),
        throw(Error)
        )).

% internal - handle end-of-file (success)
'$consult_sentence'(end_of_file, end_of_file).

% internal - handle directive (call Goal)
'$consult_sentence'((:-)(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'directive'.

% internal - handle query (call Goal)
'$consult_sentence'((?-)(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'query'.

% internal - everything else is fact or clause
'$consult_sentence'(X, Disp) :-
    % TODO: Handle reconsultation
    assertz(X), Disp = 'assert'.

% internal - call goal, with fast fail
'$consult_goal'(Goal) :-
    once(Goal) ;
    % TODO: Standard error to throw here?
    throw(failed_goal(Goal)).

% internal - finish if EOF
'$consult_next'(Stream, end_of_file).

% internal - keep going if not end of file (tail-call recursion)
'$consult_next'(Stream, X) :- '$consult_stream'(Stream).

% [...] is shortcut for consult
[H|T] :- consult([H|T]).

% :- Make private ...
