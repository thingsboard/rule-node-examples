#
# Copyright © 2018-2025 The Thingsboard Authors
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# Use the latest available ThingsBoard PE node image if 4.0.1PE is not available/suitable.
FROM thingsboard/tb-pe-node:4.0.1PE
COPY target/custom-nodes-1.0.0.jar /usr/share/thingsboard/extensions/

#USER root
#
#RUN apt-get update && apt-get install -y \
#    htop curl wget net-tools iputils-ping traceroute \
#    && rm -rf /var/lib/apt/lists/*
#
#USER thingsboard
