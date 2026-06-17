package com.simibubi.create.foundation.recipe.trie;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntIterator;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap.Entry;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import java.util.ArrayList;
import java.util.List;

public class IntArrayTrie<V> {
   private final IntArrayTrie.TrieNode<V> root = new IntArrayTrie.TrieNode<>();
   private int maxDepth = 0;
   private int nodeCount = 1;
   private int valueCount = 0;

   public int getMaxDepth() {
      return this.maxDepth;
   }

   public int getNodeCount() {
      return this.nodeCount;
   }

   public int getValueCount() {
      return this.valueCount;
   }

   public void insert(int[] key, V value) {
      IntArrayTrie.TrieNode<V> currentNode = this.root;

      for (int k : key) {
         currentNode = (IntArrayTrie.TrieNode<V>)currentNode.children.computeIfAbsent(k, k1 -> {
            this.nodeCount++;
            return new IntArrayTrie.TrieNode();
         });
      }

      currentNode.values.add(value);
      this.maxDepth = Math.max(this.maxDepth, key.length);
      this.valueCount++;
   }

   public List<V> lookup(IntSet pool) {
      List<V> result = new ArrayList<>();
      dfs(this.root, pool, result);
      return result;
   }

   private static <V> void dfs(IntArrayTrie.TrieNode<V> node, IntSet pool, List<V> out) {
      out.addAll(node.values);
      if (node.children.size() > pool.size()) {
         IntIterator var3 = pool.iterator();

         while (var3.hasNext()) {
            int key = (Integer)var3.next();
            IntArrayTrie.TrieNode<V> child = (IntArrayTrie.TrieNode<V>)node.children.get(key);
            if (child != null) {
               dfs(child, pool, out);
            }
         }
      } else {
         ObjectIterator var6 = node.children.int2ObjectEntrySet().iterator();

         while (var6.hasNext()) {
            Entry<IntArrayTrie.TrieNode<V>> entry = (Entry<IntArrayTrie.TrieNode<V>>)var6.next();
            if (pool.contains(entry.getIntKey())) {
               dfs((IntArrayTrie.TrieNode<V>)entry.getValue(), pool, out);
            }
         }
      }
   }

   @Override
   public String toString() {
      return "IntArrayTrie{maxDepth=" + this.maxDepth + ", nodeCount=" + this.nodeCount + ", valueCount=" + this.valueCount + "}";
   }

   static class TrieNode<V> {
      final Int2ObjectMap<IntArrayTrie.TrieNode<V>> children = new Int2ObjectOpenHashMap();
      final List<V> values = new ArrayList<>();
   }
}
