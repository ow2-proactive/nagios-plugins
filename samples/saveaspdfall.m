function array = saveaspdfall()

files = dir('*.fig');
for i=1:length(files)
	name = files(i).name
	open(name);
	saveas(gcf, [name '.pdf'] , 'pdf');
end

array = files;
