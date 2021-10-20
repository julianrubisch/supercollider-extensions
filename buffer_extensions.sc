VBufferCollectionTest : UnitTest {
	test_newAllocBuffers {
		var collection, server;

		collection = VBufferCollection.new(server, 4, 44100);

		this.assert(collection.buffers.every {|buffer| buffer.class == BufferViewDecorator});
		this.assert(collection.buffers.size == 4);
		this.assert(collection.views.size == 4);
	}

	test_newLoadDirectory {
		var collection, path, server;

		path = PathName.new("~/Dropbox/tensor_samples");

		this.bootServer;

		collection = VBufferCollection.new(server, path, 44100, 1);

		this.assert(collection.buffers.every {|buffer| buffer.class == BufferViewDecorator});
	}

	test_newLoadFiles {
		var collection, paths, server;

		paths = PathName.new("~/Dropbox/tensor_samples").entries;

		this.bootServer;

		collection = VBufferCollection.new(server, paths, 44100, 1);

		this.assert(collection.buffers.every {|buffer| buffer.class == BufferViewDecorator});
	}
}

VBufferCollection {
	var <buffers, views;

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
				Buffer.readChannel(server, path.fullPath, channels: [0]);
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

		buffers = buffers.collect { |buffer|
			BufferViewDecorator.new(BufferRecorderDecorator.new(buffer));
		}
	}

	// we want to lazily initialize those
	views { |parent|
		views = this.prMakeViews(parent);

		^views;
	}

	prMakeViews { |parent|
		^buffers.collect { |buffer|
			buffer.view(parent);
		}
	}
}

BufferSoundFileView : SoundFileView {
	var buffer;
	
	*new { |parent, bounds, buffer|
		^super.new.init(parent, bounds, buffer);
	}

	init { |parent, bounds, buffer|
		buffer = buffer;	

		buffer.getToFloatArray(timeout: 30, action: { |samples|
			{
				this.setData(samples);
				this.refresh;
			}.defer;
		});	
	}
}

Delegator {
	var wrapped;

	*new { |wrapped|
		^super.newCopyArgs(wrapped);
	}

	respondsTo { |aSymbol|
		^(super.respondsTo(aSymbol) || wrapped.respondsTo(aSymbol));
	}

	doesNotUnderstand { |selector ... args|
        if(wrapped.respondsTo(selector)) {
			^wrapped.performList(selector, args);
        };
		^this.superPerformList(\doesNotUnderstand, selector, args);
    }
}

BufferViewDecorator : Delegator {
	var view;

	view { |parent|
		view = BufferSoundFileView.new(parent, nil, wrapped);
		^view;
	}
}

BufferRecorderDecorator : Delegator {
	var recSynth, <>recEnd;

	*registerDefs { |server|
		SynthDef(\bufferRec, {
			|in=0, bufnum=0, rec=1|
			var sig = SoundIn.ar(in),
			stopTrig = (rec <= 0),
			phase = Phasor.ar(0, 1, 0, BufFrames.kr(bufnum));

			BufWr.ar(sig, bufnum, phase);
			SendReply.ar(K2A.ar(stopTrig), '/bufferRecEnded', [phase.poll, bufnum]);
			FreeSelf.kr(stopTrig);
		}).add;

		// see https://scsynth.org/t/looper-with-a-variable-length/818/6
		OSCdef(\bufferRecEnded, { |msg|
			// msg is ['/recEnded', nodeID, replyID, value0...]
			// so the data point is msg[3]
			var bufnum = msg[4].asInteger;
			// var pattern_key = ('pattern_' ++ bufnum).asSymbol;
			// ~rec_end[bufnum] = msg[3];  // save ending frame index
			// if(Pbindef(pattern_key).isPlaying, { Pbindef(pattern_key, \end, ~rec_end[bufnum]) })
			// callback?
			// server.cachedBufferAt(bufnum).recEnd = msg[3];
		}, '/bufferRecEnded', server.addr);
	}

	startRec {
		recSynth = Synth(\bufferRec, [\bufnum, wrapped.bufnum]);
	}

	stopRec {
		recSynth.set(\rec, 0);
	}
}