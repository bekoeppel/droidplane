# Open tasks

Known weak spots that are not bugs, written down so they don't have to be rediscovered.
None of them break anything today; each has a measurement or a concrete symptom attached.

## 1. Child nodes are a CopyOnWriteArrayList, which is quadratic for wide nodes

`MindmapNode.childMindmapNodes` is a `CopyOnWriteArrayList`, so `addChildMindmapNode()`
copies the whole backing array on every single append. A node with 5000 direct children
costs 5000 array allocations and ~12.5M element copies while the document is parsed.

It is that way for a reason: the loader appends children from its own thread while the UI
thread iterates the same list to build a `NodeColumn`, and this is what makes that safe.
An earlier attempt at locking (`synchronized (node)` in `HorizontalMindmapView.down()`)
was removed because the loader never took those monitors, so it protected nothing.

Replacing it means giving `MindmapNode` a real append/snapshot API - e.g. an `ArrayList`
plus a lock held by both the writer and by `NodeColumn` while it takes its snapshot. Worth
doing only with a profile in hand: it only bites on nodes with thousands of *direct*
children, which most mindmaps do not have.

## 2. Every MindmapNode allocates six collections whether it uses them or not

The constructor creates `childMindmapNodes`, `richTextContents`, `iconNames` and
`arrowLinkDestinationIds`, and the field initialisers add `arrowLinkDestinationNodes` and
`arrowLinkIncomingNodes`. That is six collection objects (plus their backing arrays) per
node, even though most nodes have no icons, no rich text and no arrow links at all.

Measured: a 147k node, 14 MB mindmap sits at ~116 MB of Java heap after loading, against a
512 MB limit. That headroom is what turned an earlier leak into an OutOfMemoryError rather
than just slow behaviour.

Lazy initialisation would cut it, but the getters currently hand out the live lists and
callers mutate them (`fillArrowLinks()` does `getArrowLinkDestinationNodes().add(...)`), so
it needs adder methods first and `Collections.emptyList()` from the getters while a list is
still null.

## 3. The model and the loader reach into the activity

`MindmapNode` (model) imports `MainActivity`, `NodeColumn` and `MindmapNodeLayout` (view),
and holds a `WeakReference` to each so it can tell them it changed.
`AsyncMindmapLoaderTask` touches `mainActivity` in 16 places - for `runOnUiThread`, the
loading indicator, error popups, the content resolver and resources.

The result: the model knows about the views, every notification site has to remember to
hop to the UI thread by hand, and each node can only ever have one subscriber of each kind,
which is why a re-created column silently replaces the previous one. It also makes the
loader hard to unit test - `AsyncMindmapLoaderTaskTest` only works because Robolectric can
hand it an activity that was never actually created.

The existing TODOs mark the spots:

- `AsyncMindmapLoaderTask:46` - "why is MainActivty needed here?"
- `MindmapNode:296` - "ugly that MainActivity is needed here. Would be better to introduce
  an listener interface (same for node column above)"
- `MindmapNode:82`, `MindmapNode:160` - view state and view logic living on the model
- `HorizontalMindmapView:528` - "the view should not do this"

A listener interface per event, implemented by the view layer and posted from one place,
would remove the model's dependency on the view and let the loader be tested without an
activity.
