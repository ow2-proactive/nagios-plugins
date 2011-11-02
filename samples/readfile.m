function array = readfile(file)
fid = fopen(file);
array = [];
index = 1;
tline = fgets(fid);
while ischar(tline)
    output11 = readline(tline);
    if (length(output11)==0)
    else
        output = output11;
        if (length(array)==0)
            array = output;
        else
            array(:,index) = output;
        end
        index = index + 1;
    end
    tline = fgets(fid);
end

fclose(fid);

array = array';

array = [array(:,1:8) array(:,11)];
figure;

nsamples = size(array,1);
ncurves  = size(array,2);

xtime = 1:nsamples;
compresionfactor = 144;
timeoffset = 16 * 60 + 20; % In days' long.
timeoffset = timeoffset / (60 * 24);
xtime = xtime / compresionfactor; % Now the unit is a day long.
xtime = xtime + timeoffset*ones(1,length(xtime)); 



ColorSet = [1 0.02 0.02; 0.02 1 0.02; 0.02 0.02 1; 1 0.5 0.5; 1 0.02 1; 0.02 1 1; 0.5 0.5 0.02; 0.5 0.02 0.5; 0.02 0.5 0.5];
set(gca, 'ColorOrder', ColorSet);

hold all;


plot(xtime,array);

legend('initialization','connection','clean old jobs','submission','execution','output retrieval','removal', 'disconnection', 'total time');
grid;

axis([0 timeoffset+size(array,1)/compresionfactor 0 60]);
xlabel('Time [days]');
ylabel('nTime [sec]');
titless = strrep(file, '_', ' ');
title(titless);

first = array(:,1); % Any of them will be infinite if something went wrong. We put a red dot there.
index = 1;
for ii=first'
	if ii==inf
		plot(timeoffset + index/compresionfactor,0,'ro', 'LineWidth',3);
	end
	index = index + 1;	
end


saveas(gcf, file, 'fig');