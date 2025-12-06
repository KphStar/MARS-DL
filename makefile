# === MARS Build Makefile (macOS / Linux) ===
# Build:  make jar
# Run:    make run
# Clean:  make clean

JAVAC = javac
JAVA  = java
JAR   = jar

OUT        = out
MAIN_CLASS = Mars
JAR_NAME   = Mars.jar

# Runtime resources (copied into JAR alongside classes)
RES_FILES = \
  PseudoOps.txt \
  Config.properties \
  Syscall.properties \
  Settings.properties \
  MARSlicense.txt \
  MipsXRayOpcode.xml \
  registerDatapath.xml \
  controlDatapath.xml \
  ALUcontrolDatapath.xml

RES_DIRS = docs help images

# All source roots
SRC_TOP = .
SRC_PKG = mars

# Compute the lib jars for manifest Class-Path
CP_JARS := $(shell ls lib/*.jar 2>/dev/null | sed 's/^/ /' | tr '\n' ' ')

.PHONY: all jar run clean prep

all: jar

prep:
	@mkdir -p $(OUT)

# Compile everything into out/
compile: prep
	@echo "==> Compiling sources (with JGit libraries)"
	@$(JAVAC) -cp "lib/*" -encoding ISO-8859-1 -d $(OUT) $(SRC_TOP)/Mars.java $$(find $(SRC_PKG) -name '*.java')

# Create manifest with proper Class-Path so -jar works
$(OUT)/MANIFEST.MF: prep
	@echo "==> Writing manifest with Class-Path"
	@{ \
	  echo "Manifest-Version: 1.0"; \
	  echo "Main-Class: $(MAIN_CLASS)"; \
	  echo -n "Class-Path: .$(CP_JARS)"; echo ""; \
	} > $(OUT)/MANIFEST.MF

# Stage resources into out/
resources: prep
	@echo "==> Staging resources"
	@cp -f $(RES_FILES) $(OUT)/
	@for d in $(RES_DIRS); do cp -R "$$d" $(OUT)/; done
	@cp -R mars $(OUT)/

jar: compile resources $(OUT)/MANIFEST.MF
	@echo "==> Creating $(JAR_NAME)"
	@cd $(OUT) && $(JAR) cfm ../$(JAR_NAME) MANIFEST.MF \
	  $(RES_FILES) $(RES_DIRS) mars $$(find . -name '*.class' -print)
	@echo "==> Done. Run with: make run"

run:
	@echo "==> Running $(JAR_NAME)"
	@$(JAVA) -jar $(JAR_NAME)

clean:
	@echo "==> Cleaning..."
	@rm -f $(JAR_NAME)
	@rm -rf $(OUT)
