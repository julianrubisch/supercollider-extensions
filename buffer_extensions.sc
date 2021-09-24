VBufferCollectionTest : UnitTest {
	test_newAllocBuffers {
		var collection, server;

		collection = VBufferCollection.new(server, 4, 44100);

		this.assert(collection.buffers.every {|buffer| buffer.class == Buffer});
		this.assert(collection.buffers.size == 4);
	}

	test_newLoadDirectory {
		var collection, path, server;

		path = PathName.new("~/Dropbox/tensor_samples");

		this.bootServer;

		collection = VBufferCollection.new(server, path, 44100, 1);

		this.assert(collection.buffers.every {|buffer| buffer.class == Buffer});
	}

	test_newLoadFiles {
		var collection, paths, server;

		paths = PathName.new("~/Dropbox/tensor_samples").entries;

		this.bootServer;

		collection = VBufferCollection.new(server, paths, 44100, 1);

		this.assert(collection.buffers.every {|buffer| buffer.class == Buffer});
	}
}

VBufferCollection {
	var <buffers;

	*new { |server, numBuffersOrPaths, numFrames=0, numChannels=1|
		^super.new.init(server, numBuffersOrPaths, numFrames, numChannels);
	}
	
	init { |server, numBuffersOrPaths, numFrames=0, numChannels=1|
		buffers = case
		{numBuffersOrPaths.class == PathName && {numBuffersOrPaths.isFolder}} {
			// is a directory
			numBuffersOrPaths.entries.select({ |fileOrDir| fileOrDir.isFile }).collect({
				|path|
				// Buffer.read(server, path.fullPath);

				// TODO try to monofy in place
				Buffer.readChannel(server, path.fullPath, channels: [0])
			});
		}
		{numBuffersOrPaths.isArray && {numBuffersOrPaths.every { |path| path.class == PathName && path.isFile }}} {
			// is an array of file paths
			numBuffersOrPaths.collect { |path|
				Buffer.read(server, path)
			}	
		}
		{numBuffersOrPaths.isInteger} {
			numBuffersOrPaths.collect {
				Buffer.alloc(server, numFrames, numChannels);
			}
		};
	}
}