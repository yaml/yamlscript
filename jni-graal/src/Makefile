include ../common.mk


#------------------------------------------------------------------------------
default:

clean:
	$(RM) *.h *.class *.jar


#------------------------------------------------------------------------------
%.jar: %.class manifest.txt
	jar cfm $@ manifest.txt $<

%.class: %.java
	javac $<

%.h: %.java
	javac -h . $<
