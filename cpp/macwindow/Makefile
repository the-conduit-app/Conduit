libmacwindow.dylib: macwindow.mm macwindow.h
	clang++ -dynamiclib -framework Cocoa -o libmacwindow.dylib macwindow.mm

install: libmacwindow.dylib
	cp libmacwindow.dylib "$(HOME)/Library/Application Support/Conduit/lib/"

clean:
	rm -f libmacwindow.dylib
