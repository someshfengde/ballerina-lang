/*
 *  Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 *  WSO2 Inc. licenses this file to you under the Apache License,
 *  Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 */
package io.ballerina.compiler.internal.syntax;

import io.ballerina.compiler.syntax.tree.Node;
import io.ballerina.compiler.syntax.tree.NonTerminalNode;

/**
 * Contains a set of utility methods to modify {@code NonTerminal} nodes.
 *
 * @since 2.0.0
 */
public final class TreeModifiers {

    private TreeModifiers() {
    }

    /**
     * Replaces the given target node with the replacement node and return new root node.
     *
     * @param root        root of the tree in which the target node exists
     * @param target      the node to be replaced
     * @param replacement the replacement node
     * @param <T>         the type of the root node
     * @return return the root node after replacing the target with the replacement
     */
    public static <T extends NonTerminalNode> T replace(T root, Node target, Node replacement) {
        NodeReplacer nodeReplacer = new NodeReplacer(target, replacement);
        return nodeReplacer.replace(root);
    }
}
