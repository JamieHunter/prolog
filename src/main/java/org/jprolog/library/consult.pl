% Author: Jamie Hunter, 2019
% Refer to LICENSE.TXT for copyright and license information
%
% Script to implement consult.
% TODO: This is not yet complete.
%

% load_files(:Files, +Options) recursion
load_files([], Op). % does nothing
load_files([H], Op) :- !, load_files(H, Op). % load a single file
load_files([H|T], Op) :- !, once(load_files(H, Op)), load_files(T, Op).

% load_files(File, +Options) - single file or stream specified, handle wildcard expansion
load_files(F, Op) :-
    !,
    select_option(expand(Expand), Op, RemOp, false),
    (Expand == true ->
        expand_file_name(F, FF),
        load_files(FF, [expand(false)|RemOp]) % recurse once
    ;
        '$load_file'(F, RemOp))
    .

% load_files without any options
load_files(X) :- load_files(X, []).

% Process as a stream (stream(X) option)
'$load_file'(Id, Op) :-
    select_option(stream(Stream), Op, RemOp),
    nonvar(Stream),
    !,
    (
        '$load_condition'(Id, RemOp)
    ->
        '$load_stream_in_group'(Id, RemOp, Stream),
        '$do_initialization'(Id)
    ;
        true
    )
    .

% Process as a single file (convert file to a stream)
'$load_file'(File, Op) :-
    !,
    absolute_file_name(File, AbsFile, [file_type(prolog)]),
    time_file(AbsFile, DefModified),
    current_prolog_flag(encoding, DefEncoding),
    select_option(modified(Modified), Op, Op1, DefModified),
    select_option(encoding(Encoding), Op1, Op2, DefEncoding),
    RemOp = [modified(Modified)|Op2],
    (
        '$load_condition'(AbsFile, RemOp)
    ->
        % TODO: Controlled by flags
        % Actual open/load
        open(AbsFile, read, Stream, [encoding(Encoding)]),
        catch('$file_search_scope'(AbsFile,
                '$load_stream_in_group'(AbsFile, RemOp, Stream)),
                Error, (
                    close(Stream), % make sure stream is closed on error
                    throw(Error)
                )),
        close(Stream),
        % TODO: Controlled by flags
        '$do_initialization'(AbsFile)
    ;
        true % ignore in else-case
    )
    .

% Perform conditional checks on stream being loaded
'$load_condition'(Id, Op) :-
    select_option(if(Condition), Op, RemOp, true),
    '$load_condition_check'(Condition, Id, RemOp).

'$load_condition_check'(true, Id, Op).
'$load_condition_check'(not_loaded, Id, Op) :-
        \+('$get_load_group_time'(Id, _)) .
'$load_condition_check'(changed, Id, Op) :-
    option(modified(Modified), Op, 0),
    ('$get_load_group_time'(Id, PrevTime) -> PrevTime < Modified ; true ).

% Change load group while stream is being loaded
'$load_stream_in_group'(Id, Op, Stream) :-
    option(modified(Modified), Op),
    '$load_group_scope'(Id, Modified, '$load_stream'(Op, Stream)).

% internal - given a stream, load the stream until EOF
'$load_stream'(Op, Stream) :-
    once('$consult_this'(Op, Stream, Disp)),
    '$consult_next'(Op, Stream, Disp).

% internal - consult next sentence.
'$consult_this'(Op, Stream, Disp) :-
    catch('$consult_read'(Stream, T), error(syntax_error(expected_sentence_error), Cause),
        (
            %% if in a [user] session, typing '.' by itself ends the consult
            Stream == user_input
        ->
            T = end_of_file
        ;
            throw(error(syntax_error(expected_sentence_error), Cause))
        )),
    '$consult_sentence'(Op, T, Disp).

% wrap read with prompts
'$consult_read'(Stream, T) :-
    '$consult_prompt'(Stream),
    catch(read(Stream, T), Error, (
        '$consult_prompt_end'(Stream),
        throw(Error)
        )).

'$consult_prompt'(Stream) :-
    stream_property(Stream, tty(true)) ->
        nl, writeln(user_error, 'Consulting from user. Type "." by itself to end.'),
        '$set_prompt_consult'(Stream)
        ;
        true
        .
'$consult_prompt_end'(Stream) :-
    stream_property(Stream, tty(true)) ->
        '$set_prompt_none'(Stream),
        nl, writeln(user_error, 'Finished Consulting.')
        ;
        true
        .

% internal - handle end-of-file (success)
'$consult_sentence'(Op, end_of_file, end_of_file).

% internal - handle directive (call Goal)
'$consult_sentence'(Op, ':-'(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'directive'.

% internal - handle query (call Goal)
'$consult_sentence'(Op, '?-'(Goal), Disp) :-
    '$consult_goal'(Goal), Disp = 'query'.

% internal - everything else is fact or clause
'$consult_sentence'(Op, X, Disp) :-
    % TODO: Handle reconsultation
    '$consult_assertz'(X), Disp = 'assert'.

% internal - call goal, with fast fail
'$consult_goal'(Goal) :-
    once(Goal) ;
    % TODO: Standard error to throw here?
    throw(failed_goal(Goal)).

% internal - finish if EOF
'$consult_next'(Op, Stream, end_of_file).

% internal - keep going if not end of file (tail-call recursion)
'$consult_next'(Op, Stream, X) :- '$load_stream'(Op, Stream).

% consult is a special case of load_files
consult([F]) :- !, consult(F).
consult([H|T]) :- !, once(consult(H)), consult(T).
consult(user) :- !, load_files(user, [stream(user_input)]).
consult(F) :- !, load_files(F, [expand(true)]).

% [...] is shortcut for consult
[H|T] :- consult([H|T]).

% ensure loaded if not already loaded
ensure_loaded(F) :- load_files(F, [if(not_loaded)]).

% include file into this context, effectively an inline insert
% TODO: error if called outside of a load_file
include(File) :-
    % Actual open/load
    absolute_file_name(File, AbsFile, [file_type(prolog)]),
    open(AbsFile, read, Stream),
    catch('$file_search_scope'(AbsFile, '$load_stream'([], Stream)),
            Error, (
                close(Stream), % make sure stream is closed on error
                throw(Error)
            )),
    close(Stream).

% :- Make private ...
