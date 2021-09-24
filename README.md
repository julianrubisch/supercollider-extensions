# Supercollider Classes and Extensions

## VBufferCollection

allocate empty buffers, use an existing directory, or individual files:

```supercollider
// allocate 4 buffers with length
VBufferCollection.new(server, 4, 44100).buffers;

// load all wav files from a directory
VBufferCollection.new(server, PathName.new('~/samples'), 44100, 1).buffers;

// load individual files, e.g. via PathName#entries
VBufferCollection.new(server, [PathName.new('~/samples/test1.wav'), PathName.new('~/samples/test2.wav')], 44100, 1).buffers;
```
