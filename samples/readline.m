function output = readline(str)


output = regexp(str, 'SERVICE STATUS', 'names');
if length(output)==0
else
	% Right, we have a SERVICE STATUS line now.
	output = regexp(str, 'time_initialization=(?<ini>[\d.]+)s time_connection=(?<con>[\d.]+)s time_cleaning_old_jobs=(?<cle>[\d.]+)s time_submission=(?<sub>[\d.]+)s time_execution=(?<exe>[\d.]+)s time_output_retrieval=(?<ret>[\d.]+)s time_job_removal=(?<rem>[\d.]+)s time_disconnection=(?<dis>[\d.]+)s timeout_threshold=(?<tot>[\d.]+)s time_all_warning_threshold=(?<war>[\d.]+)s time_all=(?<all>[\d.]+)s', 'names');
	if length(output)==0
		% Not a good line, there was a problem.
		output = [inf inf inf inf inf inf inf inf inf inf inf]'; 
    else
		output = str2double(struct2cell(output));
	end
end
% SERVICE STATUS: JOBID 8193 OK | time_initialization=0.003s time_connection=3.889s time_cleaning_old_jobs=0.376s time_submission=0.094s time_execution=1.576s time_output_retrieval=0.078s time_job_removal=0.102s time_disconnection=0.019s timeout_threshold=500.000s time_all_warning_threshold=500.000s time_all=5.761s


