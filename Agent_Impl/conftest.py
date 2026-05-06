# Adds the Agent_Impl directory to sys.path so that pytest can resolve
# top-level imports (config, schemas, utils, tools) regardless of which
# directory pytest is invoked from.

import sys
import os

sys.path.insert(0, os.path.dirname(__file__))