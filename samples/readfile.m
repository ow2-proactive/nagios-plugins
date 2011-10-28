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
plot(array);
legend('initi','connec','clean','submis','execu','retriev','removal', 'disconn', 'timeall');
grid;
axis([1 size(array,1) 0 max(array(:,9))]);
xlabel('Attemp (in time)');
ylabel('Time [sec]');
hold on;

first = array(:,1); % Any of them will be infinite if something went wrong. We put a red dot there.
index = 1;
for ii=first'
	if ii==inf
		plot(index,0,'ro');
	end
	index = index + 1;	
end
