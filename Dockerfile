#
# SPDX-FileCopyrightText: Copyright The ThingsBoard Authors
# SPDX-License-Identifier: Apache-2.0
#

FROM thingsboard/tb-node:4.3.1.8
COPY target/rule-engine-1.0.0-custom-nodes.jar /usr/share/thingsboard/extensions/
